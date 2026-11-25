package com.musa.chat.dto.response;
import com.musa.chat.entity.Message;
import java.time.Instant;
import java.util.UUID;
public record MessageResponse(UUID id, UUID conversationId, UUID senderId, String content, Instant createdAt) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(message.getId(), message.getConversationId(), message.getSenderId(),
                message.getContent(), message.getCreatedAt());
    }
}
