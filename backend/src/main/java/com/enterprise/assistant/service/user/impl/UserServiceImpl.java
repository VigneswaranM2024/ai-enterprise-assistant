package com.enterprise.assistant.service.user.impl;

import com.enterprise.assistant.domain.department.Department;
import com.enterprise.assistant.domain.tenant.Tenant;
import com.enterprise.assistant.domain.user.Permission;
import com.enterprise.assistant.domain.user.Role;
import com.enterprise.assistant.domain.user.SecurityClassification;
import com.enterprise.assistant.domain.user.User;
import com.enterprise.assistant.dto.request.CreateUserRequest;
import com.enterprise.assistant.dto.request.UpdateUserRequest;
import com.enterprise.assistant.dto.request.UpdateUserStatusRequest;
import com.enterprise.assistant.dto.response.PageResponse;
import com.enterprise.assistant.dto.response.UserResponse;
import com.enterprise.assistant.exception.ResourceConflictException;
import com.enterprise.assistant.exception.ResourceNotFoundException;
import com.enterprise.assistant.repository.department.DepartmentRepository;
import com.enterprise.assistant.repository.tenant.TenantRepository;
import com.enterprise.assistant.repository.user.RoleRepository;
import com.enterprise.assistant.repository.user.UserRepository;
import com.enterprise.assistant.service.audit.AuditLogService;
import com.enterprise.assistant.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Transactional Implementation of User Service.
 * Enforces strict tenant isolation, protected property bounds, and audit logging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getTenantUsers(UUID tenantId, String query, Boolean isActive, Pageable pageable) {
        log.debug("Searching tenant users for tenant: {}, query: '{}'", tenantId, query);
        Page<User> page = userRepository.searchTenantUsers(tenantId, query, isActive, pageable);
        Page<UserResponse> responsePage = page.map(this::mapToUserResponse);
        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID tenantId, UUID userId) {
        User user = userRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return mapToUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(UUID tenantId, UUID userId) {
        return getUserById(tenantId, userId);
    }

    @Override
    @Transactional
    public UserResponse createUser(UUID tenantId, CreateUserRequest request, UUID actorId) {
        log.info("Creating new user: {} for tenant: {}", request.email(), tenantId);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + tenantId));

        User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;

        if (userRepository.existsByTenantIdAndEmail(tenantId, request.email())) {
            throw new ResourceConflictException("Email '" + request.email() + "' is already registered in this tenant");
        }

        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                    .filter(d -> d.getTenant().getId().equals(tenantId))
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found in this tenant"));
        }

        Set<Role> roles = new HashSet<>();
        if (request.roleNames() != null && !request.roleNames().isEmpty()) {
            for (String rawName : request.roleNames()) {
                String roleName = rawName.startsWith("ROLE_") ? rawName : "ROLE_" + rawName.toUpperCase();
                Role role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role '" + roleName + "' not found in this tenant"));
                roles.add(role);
            }
        } else {
            Role defaultRole = roleRepository.findByTenantIdAndName(tenantId, "ROLE_EMPLOYEE")
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .tenant(tenant)
                            .name("ROLE_EMPLOYEE")
                            .description("Default Employee Role")
                            .isSystemRole(true)
                            .build()));
            roles.add(defaultRole);
        }

        User newUser = User.builder()
                .tenant(tenant)
                .department(department)
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .jobTitle(request.jobTitle())
                .securityClassification(SecurityClassification.INTERNAL)
                .isActive(true)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(newUser);

        auditLogService.logEvent(tenant, actor, "USER_CREATED", "USER", savedUser.getId(), "{\"email\":\"" + savedUser.getEmail() + "\"}", "SUCCESS");

        return mapToUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID tenantId, UUID userId, UpdateUserRequest request, UUID actorId) {
        log.info("Updating profile for user ID: {} in tenant: {}", userId, tenantId);

        User user = userRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }

        if (request.jobTitle() != null) {
            user.setJobTitle(request.jobTitle());
        }

        if (request.departmentId() != null) {
            Department department = departmentRepository.findById(request.departmentId())
                    .filter(d -> d.getTenant().getId().equals(tenantId))
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found in this tenant"));
            user.setDepartment(department);
        }

        User updatedUser = userRepository.save(user);

        auditLogService.logEvent(user.getTenant(), actor, "USER_UPDATED", "USER", updatedUser.getId(), "{\"email\":\"" + updatedUser.getEmail() + "\"}", "SUCCESS");

        return mapToUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(UUID tenantId, UUID userId, UpdateUserStatusRequest request, UUID actorId) {
        log.info("Changing active status for user ID: {} to {}", userId, request.isActive());

        User user = userRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;

        user.setIsActive(request.isActive());
        User updatedUser = userRepository.save(user);

        String action = request.isActive() ? "USER_ACTIVATED" : "USER_DEACTIVATED";
        auditLogService.logEvent(user.getTenant(), actor, action, "USER", updatedUser.getId(), "{\"email\":\"" + updatedUser.getEmail() + "\"}", "SUCCESS");

        return mapToUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(UUID tenantId, UUID userId, UUID actorId) {
        log.info("Deleting user ID: {} from tenant: {}", userId, tenantId);

        User user = userRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;

        userRepository.delete(user);

        auditLogService.logEvent(user.getTenant(), actor, "USER_DELETED", "USER", userId, "{\"email\":\"" + user.getEmail() + "\"}", "SUCCESS");
    }

    @Override
    @Transactional
    public UserResponse assignRoleToUser(UUID tenantId, UUID userId, UUID roleId, UUID actorId) {
        User user = userRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Role role = roleRepository.findByTenantIdAndId(tenantId, roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found in this tenant"));

        User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;

        user.getRoles().add(role);
        User updatedUser = userRepository.save(user);

        auditLogService.logEvent(user.getTenant(), actor, "ROLE_ASSIGNED", "USER", userId, "{\"roleName\":\"" + role.getName() + "\"}", "SUCCESS");

        return mapToUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse removeRoleFromUser(UUID tenantId, UUID userId, UUID roleId, UUID actorId) {
        User user = userRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Role role = roleRepository.findByTenantIdAndId(tenantId, roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found in this tenant"));

        User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;

        user.getRoles().remove(role);
        User updatedUser = userRepository.save(user);

        auditLogService.logEvent(user.getTenant(), actor, "ROLE_REMOVED", "USER", userId, "{\"roleName\":\"" + role.getName() + "\"}", "SUCCESS");

        return mapToUserResponse(updatedUser);
    }

    private UserResponse mapToUserResponse(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Set<String> permissions = user.getRoles().stream()
                .filter(r -> r.getPermissions() != null)
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        UUID deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : null;

        return new UserResponse(
                user.getId(),
                user.getTenant().getId(),
                user.getTenant().getSlug(),
                user.getEmail(),
                user.getFullName(),
                user.getJobTitle(),
                deptId,
                deptName,
                user.getSecurityClassification().name(),
                user.getIsActive(),
                user.getLastLoginAt(),
                roles,
                permissions,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
