package com.enterprise.assistant.service.role;

import com.enterprise.assistant.dto.request.AssignRolesRequest;
import com.enterprise.assistant.dto.request.CreateRoleRequest;
import com.enterprise.assistant.dto.request.UpdateRoleRequest;
import com.enterprise.assistant.dto.response.RoleResponse;
import com.enterprise.assistant.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service Contract for Tenant Role Management & Role Assignment Operations.
 */
public interface RoleService {

    List<RoleResponse> getTenantRoles(UUID tenantId);

    RoleResponse getRoleById(UUID tenantId, UUID roleId);

    RoleResponse createRole(UUID tenantId, CreateRoleRequest request, UUID actorId);

    RoleResponse updateRole(UUID tenantId, UUID roleId, UpdateRoleRequest request, UUID actorId);

    void deleteRole(UUID tenantId, UUID roleId, UUID actorId);

    UserResponse assignRolesToUser(UUID tenantId, UUID userId, AssignRolesRequest request);
}
