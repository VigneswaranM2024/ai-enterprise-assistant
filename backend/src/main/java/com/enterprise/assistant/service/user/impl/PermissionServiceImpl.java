package com.enterprise.assistant.service.user.impl;

import com.enterprise.assistant.domain.user.Permission;
import com.enterprise.assistant.dto.request.CreatePermissionRequest;
import com.enterprise.assistant.dto.response.PermissionResponse;
import com.enterprise.assistant.exception.ResourceConflictException;
import com.enterprise.assistant.repository.user.PermissionRepository;
import com.enterprise.assistant.service.user.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Transactional Implementation of Permission Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::mapToPermissionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        String code = request.code().toUpperCase();
        log.info("Creating system permission with code: {}", code);

        if (permissionRepository.existsByCode(code)) {
            throw new ResourceConflictException("Permission with code '" + code + "' already exists");
        }

        Permission permission = Permission.builder()
                .code(code)
                .category(request.category().toUpperCase())
                .description(request.description())
                .build();

        Permission savedPermission = permissionRepository.save(permission);
        return mapToPermissionResponse(savedPermission);
    }

    private PermissionResponse mapToPermissionResponse(Permission p) {
        return new PermissionResponse(
                p.getId(),
                p.getCode(),
                p.getCategory(),
                p.getDescription(),
                p.getCreatedAt()
        );
    }
}
