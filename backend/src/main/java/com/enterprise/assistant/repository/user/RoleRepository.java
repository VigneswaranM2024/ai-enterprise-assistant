package com.enterprise.assistant.repository.user;

import com.enterprise.assistant.domain.user.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Role Entity.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findByTenantIdAndName(UUID tenantId, String name);

    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findByTenantIdAndId(UUID tenantId, UUID id);

    @EntityGraph(attributePaths = {"permissions"})
    List<Role> findAllByTenantId(UUID tenantId);

    boolean existsByTenantIdAndName(UUID tenantId, String name);
}
