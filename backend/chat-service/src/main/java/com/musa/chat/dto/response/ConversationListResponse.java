package com.musa.chat.dto.response;
import java.util.List;
public record ConversationListResponse(List<ConversationResponse> items, boolean hasMore, Integer nextPage) {
    public ConversationListResponse { items = List.copyOf(items); }
}
