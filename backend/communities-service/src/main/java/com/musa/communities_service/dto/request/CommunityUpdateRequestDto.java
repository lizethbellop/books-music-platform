package com.musa.communities_service.dto.request;

import com.musa.communities_service.enums.CommunityType;
import com.musa.communities_service.enums.CommunityVisibility;
import jakarta.validation.constraints.Size;

/** DTO de solicitud para actualizar la información de una comunidad existente. */
public record CommunityUpdateRequestDto(

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String description,
        CommunityType type,
        CommunityVisibility visibility
) {}
