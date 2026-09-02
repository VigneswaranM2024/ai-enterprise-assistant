package com.enterprise.assistant.service.chat;

import com.enterprise.assistant.dto.request.CreateSessionRequest;
import com.enterprise.assistant.dto.request.SendMessageRequest;
import com.enterprise.assistant.dto.response.ChatMessageResponse;
import com.enterprise.assistant.dto.response.ChatSessionDetailResponse;
import com.enterprise.assistant.dto.response.ChatSessionResponse;
import com.enterprise.assistant.security.user.UserPrincipal;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for conversation memory and chat session management.
 */
public interface ChatService {

    ChatSessionResponse createSession(UserPrincipal principal, CreateSessionRequest request);

    List<ChatSessionResponse> getUserSessions(UserPrincipal principal);

    ChatSessionDetailResponse getSessionDetails(UserPrincipal principal, UUID sessionId);

    void deleteSession(UserPrincipal principal, UUID sessionId);

    ChatMessageResponse sendMessage(UserPrincipal principal, UUID sessionId, SendMessageRequest request);
}
