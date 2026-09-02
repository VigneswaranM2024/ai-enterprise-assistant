package com.enterprise.assistant.controller.chat;

import com.enterprise.assistant.domain.chat.ChatSessionStatus;
import com.enterprise.assistant.domain.chat.MessageRole;
import com.enterprise.assistant.dto.request.CreateSessionRequest;
import com.enterprise.assistant.dto.request.SendMessageRequest;
import com.enterprise.assistant.dto.response.*;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.chat.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    private ChatController controller;

    private UserPrincipal userPrincipal;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        controller = new ChatController(chatService);
        sessionId = UUID.randomUUID();

        userPrincipal = new UserPrincipal(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "tenant-a",
                "user@acme.com",
                "pwd",
                "Acme User",
                "INTERNAL",
                "ENGINEERING",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"), new SimpleGrantedAuthority("AI_QUERY"))
        );
    }

    @Test
    void createSession_Returns201Created() {
        ChatSessionResponse mockResp = new ChatSessionResponse(
                sessionId, "New Session", ChatSessionStatus.ACTIVE, OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(chatService.createSession(eq(userPrincipal), any())).thenReturn(mockResp);

        CreateSessionRequest req = new CreateSessionRequest("New Session");
        ResponseEntity<ApiResponse<ChatSessionResponse>> resp = controller.createSession(userPrincipal, req);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isSuccess());
        assertEquals(sessionId, resp.getBody().getData().id());
    }

    @Test
    void getUserSessions_Returns200OK() {
        ChatSessionResponse mockResp = new ChatSessionResponse(
                sessionId, "Active Session", ChatSessionStatus.ACTIVE, OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(chatService.getUserSessions(userPrincipal)).thenReturn(List.of(mockResp));

        ResponseEntity<ApiResponse<List<ChatSessionResponse>>> resp = controller.getUserSessions(userPrincipal);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().getData().size());
    }

    @Test
    void getSessionDetails_Returns200OK() {
        ChatSessionDetailResponse mockDetail = new ChatSessionDetailResponse(
                sessionId, "Active Session", ChatSessionStatus.ACTIVE, OffsetDateTime.now(), OffsetDateTime.now(), Collections.emptyList()
        );
        when(chatService.getSessionDetails(userPrincipal, sessionId)).thenReturn(mockDetail);

        ResponseEntity<ApiResponse<ChatSessionDetailResponse>> resp = controller.getSessionDetails(userPrincipal, sessionId);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(sessionId, resp.getBody().getData().id());
    }

    @Test
    void deleteSession_Returns200OK() {
        doNothing().when(chatService).deleteSession(userPrincipal, sessionId);

        ResponseEntity<ApiResponse<Void>> resp = controller.deleteSession(userPrincipal, sessionId);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(chatService).deleteSession(userPrincipal, sessionId);
    }

    @Test
    void sendMessage_Returns200OK() {
        ChatMessageDTO userMsg = new ChatMessageDTO(UUID.randomUUID(), MessageRole.USER, "Hello", Collections.emptyList(), OffsetDateTime.now());
        ChatMessageDTO assistantMsg = new ChatMessageDTO(UUID.randomUUID(), MessageRole.ASSISTANT, "Hi there", Collections.emptyList(), OffsetDateTime.now());
        ChatMessageResponse mockMsgResp = new ChatMessageResponse(sessionId, userMsg, assistantMsg, Collections.emptyList(), 0, "openai/gpt-oss-20b");

        when(chatService.sendMessage(eq(userPrincipal), eq(sessionId), any())).thenReturn(mockMsgResp);

        SendMessageRequest req = new SendMessageRequest("Hello");
        ResponseEntity<ApiResponse<ChatMessageResponse>> resp = controller.sendMessage(userPrincipal, sessionId, req);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(sessionId, resp.getBody().getData().sessionId());
    }
}
