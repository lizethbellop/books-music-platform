package com.musa.notifications.dto;

import com.musa.notifications.entity.NotificationType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record NotificationEvent(
        @Min(1) @Max(1)
        int version,

        @NotNull
        UUID eventId,

        @NotNull
        NotificationType type,

        @NotNull
        UUID recipientId,

        @NotNull
        UUID actorId,

        @NotNull
        UUID targetId
) {
}