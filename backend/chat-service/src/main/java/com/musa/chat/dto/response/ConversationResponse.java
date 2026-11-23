package com.musa.chat.dto.response;
import com.musa.chat.entity.ConversationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record ConversationResponse(UUID id, ConversationType type, Instant createdAt,
        List<UUID> participantIds, String name, UUID ownerId) {
    public ConversationResponse { participantIds = List.copyOf(participantIds); }
    public ConversationResponse(UUID id, ConversationType type, Instant createdAt, List<UUID> participantIds) {
        this(id, type, createdAt, participantIds, null, null);
    }
}
