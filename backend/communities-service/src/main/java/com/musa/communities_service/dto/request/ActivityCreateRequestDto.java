package com.musa.communities_service.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/** DTO de solicitud para crear una nueva actividad en una comunidad. */
public record ActivityCreateRequestDto(

        @NotBlank(message = "El título de la actividad es obligatorio")
        @Size(max = 150, message = "El título no puede superar los 150 caracteres")
        String title,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String description,

        @Size(max = 255, message = "La ubicación no puede superar los 255 caracteres")
        String location,

        @NotNull(message = "La fecha y hora de inicio son obligatorias")
        @Future(message = "La fecha de inicio debe ser una fecha futura")
        OffsetDateTime startsAt,

        OffsetDateTime endsAt
) {}
