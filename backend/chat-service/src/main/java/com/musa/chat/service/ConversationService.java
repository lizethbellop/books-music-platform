package com.musa.chat.service;

import com.musa.chat.dto.request.CreatePrivateConversationRequest;
import com.musa.chat.dto.response.ConversationResponse;

import java.util.UUID;

public interface ConversationService {

    ConversationResponse createPrivateConversation(
            UUID currentUserId,
            CreatePrivateConversationRequest request
    );
}