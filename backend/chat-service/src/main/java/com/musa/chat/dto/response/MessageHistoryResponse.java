package com.musa.chat.dto.response;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record MessageHistoryResponse(List<MessageResponse> items, boolean hasMore,
        Instant nextCreatedAt, UUID nextId) {
    public MessageHistoryResponse { items = List.copyOf(items); }
}
