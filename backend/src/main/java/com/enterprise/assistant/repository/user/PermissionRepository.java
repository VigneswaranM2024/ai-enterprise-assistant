package com.enterprise.assistant.repository.user;

import com.enterprise.assistant.domain.user.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Permission Entity.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCode(String code);

    Set<Permission> findByCodeIn(Set<String> codes);

    boolean existsByCode(String code);
}
