package com.enterprise.assistant.repository.chat;

import com.enterprise.assistant.domain.chat.ChatSession;
import com.enterprise.assistant.domain.chat.ChatSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ChatSession management enforcing tenant and user boundaries.
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    Optional<ChatSession> findByIdAndTenantIdAndUserId(UUID id, UUID tenantId, UUID userId);

    Optional<ChatSession> findByIdAndTenantIdAndUserIdAndStatus(UUID id, UUID tenantId, UUID userId, ChatSessionStatus status);

    List<ChatSession> findByTenantIdAndUserIdAndStatusOrderByUpdatedAtDesc(UUID tenantId, UUID userId, ChatSessionStatus status);

    long countByTenantIdAndUserIdAndStatus(UUID tenantId, UUID userId, ChatSessionStatus status);

    long countByTenantIdAndStatus(UUID tenantId, ChatSessionStatus status);
}
