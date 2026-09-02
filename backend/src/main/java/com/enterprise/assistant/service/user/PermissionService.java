package com.enterprise.assistant.service.user;

import com.enterprise.assistant.dto.request.CreatePermissionRequest;
import com.enterprise.assistant.dto.response.PermissionResponse;

import java.util.List;

/**
 * Service Contract for Permission Catalog Management.
 */
public interface PermissionService {

    List<PermissionResponse> getAllPermissions();

    PermissionResponse createPermission(CreatePermissionRequest request);
}
