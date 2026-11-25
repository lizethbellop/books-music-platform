package com.musa.chat.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePrivateConversationRequest(
        @NotNull(message = "Debes indicar el usuario con quien deseas conversar")
        UUID recipientId
) {
}