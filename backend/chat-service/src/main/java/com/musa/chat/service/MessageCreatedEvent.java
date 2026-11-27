package com.musa.chat.service;
import com.musa.chat.dto.response.MessageResponse;
import java.util.List;
import java.util.UUID;
public record MessageCreatedEvent(MessageResponse message, List<UUID> participantIds) {
    public MessageCreatedEvent { participantIds = List.copyOf(participantIds); }
}
