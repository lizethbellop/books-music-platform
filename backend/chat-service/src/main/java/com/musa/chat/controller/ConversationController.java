package com.musa.chat.controller;

import com.musa.chat.dto.request.CreatePrivateConversationRequest;
import com.musa.chat.dto.response.ConversationResponse;
import com.musa.chat.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat/conversations")
public class ConversationController {
    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/private")
    public ConversationResponse createPrivateConversation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePrivateConversationRequest request
    ) {
        return conversationService.createPrivateConversation(
                UUID.fromString(jwt.getSubject()), request);
    }
}
