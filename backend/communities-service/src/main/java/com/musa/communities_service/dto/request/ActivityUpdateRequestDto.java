package com.musa.communities_service.dto.request;

import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record ActivityUpdateRequestDto(
        @Size(max = 150, message = "El título no puede superar los 150 caracteres")
        String title,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String description,

        @Size(max = 255, message = "La ubicación no puede superar los 255 caracteres")
        String location,

        OffsetDateTime startsAt,
        OffsetDateTime endsAt
) {}