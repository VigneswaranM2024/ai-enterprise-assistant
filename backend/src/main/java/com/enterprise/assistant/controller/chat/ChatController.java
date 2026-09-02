package com.enterprise.assistant.controller.chat;

import com.enterprise.assistant.dto.request.CreateSessionRequest;
import com.enterprise.assistant.dto.request.SendMessageRequest;
import com.enterprise.assistant.dto.response.*;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.chat.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for conversation memory and chat session management.
 */
@RestController
@RequestMapping("/api/v1/chat/sessions")
@RequiredArgsConstructor
@Tag(name = "Chat Management", description = "Endpoints for multi-turn chat sessions and conversation memory")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @PreAuthorize("hasAuthority('AI_QUERY')")
    @Operation(summary = "Create chat session", description = "Creates a new tenant- and user-isolated chat session")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> createSession(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody(required = false) CreateSessionRequest request
    ) {
        ChatSessionResponse response = chatService.createSession(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Chat session created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AI_QUERY')")
    @Operation(summary = "List user chat sessions", description = "Retrieves active chat sessions for the authenticated user")
    public ResponseEntity<ApiResponse<List<ChatSessionResponse>>> getUserSessions(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        List<ChatSessionResponse> sessions = chatService.getUserSessions(currentUser);
        return ResponseEntity.ok(ApiResponse.success("User chat sessions retrieved successfully", sessions));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('AI_QUERY')")
    @Operation(summary = "Get chat session details", description = "Retrieves details and message history for a specific chat session")
    public ResponseEntity<ApiResponse<ChatSessionDetailResponse>> getSessionDetails(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id
    ) {
        ChatSessionDetailResponse details = chatService.getSessionDetails(currentUser, id);
        return ResponseEntity.ok(ApiResponse.success("Chat session details retrieved successfully", details));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('AI_QUERY')")
    @Operation(summary = "Delete chat session", description = "Soft-deletes a chat session for the authenticated user")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id
    ) {
        chatService.deleteSession(currentUser, id);
        return ResponseEntity.ok(ApiResponse.success("Chat session deleted successfully", null));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAuthority('AI_QUERY')")
    @Operation(summary = "Send message in chat session", description = "Sends a new user message turn, loads recent memory, executes RAG, and returns assistant answer with citations")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request
    ) {
        ChatMessageResponse response = chatService.sendMessage(currentUser, id, request);
        return ResponseEntity.ok(ApiResponse.success("Message processed successfully", response));
    }
}
