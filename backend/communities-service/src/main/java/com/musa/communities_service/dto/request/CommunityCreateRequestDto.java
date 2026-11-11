package com.musa.communities_service.dto.request;

import com.musa.communities_service.enums.CommunityType;
import com.musa.communities_service.enums.CommunityVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTO de solicitud para la creación de una nueva comunidad. */
public record CommunityCreateRequestDto(

        @NotBlank(message = "El nombre de la comunidad es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String description,

        @NotNull(message = "El tipo de comunidad es obligatorio")
        CommunityType type,

        @NotNull(message = "La visibilidad es obligatoria")
        CommunityVisibility visibility
) {}
