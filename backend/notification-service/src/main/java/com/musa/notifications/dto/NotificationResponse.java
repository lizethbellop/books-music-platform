package com.musa.notifications.dto;
import com.musa.notifications.entity.Notification;
import java.time.Instant;
import java.util.UUID;
public record NotificationResponse(UUID id, UUID actorId, String type, String title, String body,
        String targetType, UUID targetId, Instant createdAt, Instant readAt) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getActorId(), n.getType().name(), n.getTitle(),
                n.getBody(), n.getTargetType().name(), n.getTargetId(), n.getCreatedAt(), n.getReadAt());
    }
}
