package com.enterprise.assistant.repository.user;

import com.enterprise.assistant.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for User Entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"roles", "department", "tenant"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"roles", "department", "tenant"})
    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    @EntityGraph(attributePaths = {"roles", "department", "tenant"})
    Optional<User> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND " +
           "(CAST(:query AS string) IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%'))) AND " +
           "(:isActive IS NULL OR u.isActive = :isActive)")
    @EntityGraph(attributePaths = {"roles", "department"})
    Page<User> searchTenantUsers(@Param("tenantId") UUID tenantId,
                                 @Param("query") String query,
                                 @Param("isActive") Boolean isActive,
                                 Pageable pageable);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndIsActive(UUID tenantId, Boolean isActive);
}
