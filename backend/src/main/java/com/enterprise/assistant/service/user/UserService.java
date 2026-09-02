package com.enterprise.assistant.service.user;

import com.enterprise.assistant.dto.request.CreateUserRequest;
import com.enterprise.assistant.dto.request.UpdateUserRequest;
import com.enterprise.assistant.dto.request.UpdateUserStatusRequest;
import com.enterprise.assistant.dto.response.PageResponse;
import com.enterprise.assistant.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service Contract for Tenant User Management Operations.
 */
public interface UserService {

    PageResponse<UserResponse> getTenantUsers(UUID tenantId, String query, Boolean isActive, Pageable pageable);

    UserResponse getUserById(UUID tenantId, UUID userId);

    UserResponse getCurrentUserProfile(UUID tenantId, UUID userId);

    UserResponse createUser(UUID tenantId, CreateUserRequest request, UUID actorId);

    UserResponse updateUser(UUID tenantId, UUID userId, UpdateUserRequest request, UUID actorId);

    UserResponse updateUserStatus(UUID tenantId, UUID userId, UpdateUserStatusRequest request, UUID actorId);

    void deleteUser(UUID tenantId, UUID userId, UUID actorId);

    UserResponse assignRoleToUser(UUID tenantId, UUID userId, UUID roleId, UUID actorId);

    UserResponse removeRoleFromUser(UUID tenantId, UUID userId, UUID roleId, UUID actorId);
}
