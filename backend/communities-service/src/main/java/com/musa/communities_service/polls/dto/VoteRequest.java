package com.musa.communities_service.polls.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VoteRequest(

        @NotNull(message = "Selecciona una opción")
        UUID optionId

) {
}