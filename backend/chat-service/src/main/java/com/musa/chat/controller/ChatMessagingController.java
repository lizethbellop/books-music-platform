package com.musa.chat.controller;
import com.musa.chat.dto.request.SendMessageRequest;
import com.musa.chat.dto.response.*;
import com.musa.chat.service.ChatMessagingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat/conversations")
public class ChatMessagingController {
    private final ChatMessagingService service;
    public ChatMessagingController(ChatMessagingService service) { this.service = service; }
    @GetMapping
    public ConversationListResponse list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.list(UUID.fromString(jwt.getSubject()), page, size);
    }
    @GetMapping("/{conversationId}/messages")
    public MessageHistoryResponse history(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant beforeCreatedAt,
            @RequestParam(required = false) UUID beforeId) {
        return service.history(UUID.fromString(jwt.getSubject()), conversationId, size, beforeCreatedAt, beforeId);
    }
    @PostMapping("/{conversationId}/messages")
    public MessageResponse send(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return service.send(UUID.fromString(jwt.getSubject()), conversationId, request);
    }
}
