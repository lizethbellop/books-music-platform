package com.musa.chat.service;
import java.util.List;
import java.util.UUID;
public record GroupChangedEvent(Action action, UUID conversationId, UUID actorId,
        List<UUID> affectedUserIds, List<UUID> audience) {
    public enum Action { CREATED, MEMBER_ADDED, MEMBER_REMOVED, OWNER_CHANGED, RENAMED, LEFT, CLOSED }
    public GroupChangedEvent { affectedUserIds = List.copyOf(affectedUserIds); audience = List.copyOf(audience); }
}
