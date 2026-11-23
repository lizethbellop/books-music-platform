package com.musa.chat.messaging;
import java.util.UUID;
public record NotificationEvent(int version, UUID eventId, String type, UUID recipientId, UUID actorId, UUID targetId) {}
