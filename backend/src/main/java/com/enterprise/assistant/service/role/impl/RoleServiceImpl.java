package com.enterprise.assistant.service.role.impl;

import com.enterprise.assistant.domain.tenant.Tenant;
import com.enterprise.assistant.domain.user.Permission;
import com.enterprise.assistant.domain.user.Role;
import com.enterprise.assistant.domain.user.User;
import com.enterprise.assistant.dto.request.AssignRolesRequest;
import com.enterprise.assistant.dto.request.CreateRoleRequest;
import com.enterprise.assistant.dto.request.UpdateRoleRequest;
import com.enterprise.assistant.dto.response.RoleResponse;
import com.enterprise.assistant.dto.response.UserResponse;
import com.enterprise.assistant.exception.ResourceConflictException;
import com.enterprise.assistant.exception.ResourceNotFoundException;
import com.enterprise.assistant.repository.tenant.TenantRepository;
import com.enterprise.assistant.repository.user.PermissionRepository;
import com.enterprise.assistant.repository.user.RoleRepository;
import com.enterprise.assistant.repository.user.UserRepository;
import com.enterprise.assistant.service.audit.AuditLogService;
import com.enterprise.assistant.service.role.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Transactional Implementation of Role Service.
 * Enforces strict tenant isolation, system role protection, and audit logging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getTenantRoles(UUID tenantId) {
        log.debug("Fetching all roles for tenant: {}", tenantId);
        return roleRepository.findAllByTenantId(tenantId).stream()
                .map(this::mapToRoleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID tenantId, UUID roleId) {
        Role role = roleRepository.findByTenantIdAndId(tenantId, roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + roleId));
        return mapToRoleResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse createRole(UUID tenantId, CreateRoleRequest request, UUID actorId) {
        log.info("Creating new custom role: {} for tenant: {}", request.name(), tenantId);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + tenantId));

        User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;

        String roleName = request.name().startsWith("ROLE_") ? request.name() : "ROLE_" + request.name().toUpperCase();

        if (roleRepository.existsByTenantIdAndName(tenantId, roleName)) {
            throw new ResourceConflictException("Role '" + roleName + "' already exists in this tenant");
        }

        Set<Permission> permissions = new HashSet<>();
        if (request.permissionCodes() != null && !request.permissionCodes().isEmpty()) {
            permissions = permissionRepository.findByCodeIn(request.permissionCodes());
        }

        Role newRole = Role.builder()
                .tenant(tenant)
                .name(roleName)
                .description(request.description())
                .isSystemRole(false)
                .permissions(permissions)
                .build();

        Role savedRole = roleRepository.save(newRole);
        log.info("Successfully created role: {} (ID: {})", savedRole.getName(), savedRole.getId());

        auditLogService.logEvent(tenant, actor, "ROLE_CREATED", "ROLE", savedRole.getId(), "{\"roleName\":\"" + roleName + "\"}", "SUCCESS");

        return mapToRoleResponse(savedRole);
    }

    @Override
    @Transactional
    public RoleResponse updateRole(UUID tenantId, UUID roleId, UpdateRoleRequest request, UUID actorId) {
        Role role = roleRepository.findByTenantIdAndId(tenantId, roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + roleId));

        User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;

        if (request.description() != null) {
            role.setDescription(request.description());
        }

        if (request.permissionCodes() != null) {
            Set<Permission> permissions = permissionRepository.findByCodeIn(request.permissionCodes());
            role.setPermissions(permissions);
        }

        Role updatedRole = roleRepository.save(role);

        auditLogService.logEvent(role.getTenant(), actor, "ROLE_UPDATED", "ROLE", updatedRole.getId(), "{\"roleName\":\"" + updatedRole.getName() + "\"}", "SUCCESS");

        return mapToRoleResponse(updatedRole);
    }

    @Override
    @Transactional
    public void deleteRole(UUID tenantId, UUID roleId, UUID actorId) {
        Role role = roleRepository.findByTenantIdAndId(tenantId, roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + roleId));

        if (Boolean.TRUE.equals(role.getIsSystemRole()) || "ROLE_ADMIN".equals(role.getName()) || "ROLE_EMPLOYEE".equals(role.getName())) {
            throw new ResourceConflictException("System critical role '" + role.getName() + "' cannot be deleted.");
        }

        User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;

        roleRepository.delete(role);
        log.info("Successfully deleted role ID: {} from tenant: {}", roleId, tenantId);

        auditLogService.logEvent(role.getTenant(), actor, "ROLE_DELETED", "ROLE", roleId, "{\"roleName\":\"" + role.getName() + "\"}", "SUCCESS");
    }

    @Override
    @Transactional
    public UserResponse assignRolesToUser(UUID tenantId, UUID userId, AssignRolesRequest request) {
        log.info("Assigning roles: {} to user ID: {} for tenant: {}", request.roleNames(), userId, tenantId);

        User user = userRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Set<Role> rolesToAssign = new HashSet<>();
        for (String rawRoleName : request.roleNames()) {
            String roleName = rawRoleName.startsWith("ROLE_") ? rawRoleName : "ROLE_" + rawRoleName.toUpperCase();
            Role role = roleRepository.findByTenantIdAndName(tenantId, roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role '" + roleName + "' not found in this tenant"));
            rolesToAssign.add(role);
        }

        user.setRoles(rolesToAssign);
        User updatedUser = userRepository.save(user);

        log.info("Successfully updated roles for user ID: {}", updatedUser.getId());
        return mapToUserResponse(updatedUser);
    }

    private RoleResponse mapToRoleResponse(Role role) {
        Set<String> permissionCodes = role.getPermissions() != null
                ? role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet())
                : Collections.emptySet();

        return new RoleResponse(
                role.getId(),
                role.getTenant().getId(),
                role.getName(),
                role.getDescription(),
                role.getIsSystemRole(),
                permissionCodes,
                role.getCreatedAt()
        );
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
