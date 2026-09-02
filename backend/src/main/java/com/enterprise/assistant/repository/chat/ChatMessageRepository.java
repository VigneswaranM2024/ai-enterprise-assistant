package com.enterprise.assistant.repository.chat;

import com.enterprise.assistant.domain.chat.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ChatMessage turns within sessions.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    List<ChatMessage> findBySessionIdOrderByCreatedAtDesc(UUID sessionId, Pageable pageable);

    long countBySessionId(UUID sessionId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.session.tenant.id = :tenantId")
    long countByTenantId(@org.springframework.data.repository.query.Param("tenantId") UUID tenantId);

    void deleteBySessionId(UUID sessionId);
}
