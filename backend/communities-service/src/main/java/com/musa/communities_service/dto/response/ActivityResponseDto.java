package com.musa.communities_service.dto.response;

import com.musa.communities_service.enums.ActivityStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** DTO de respuesta con la información detallada de una actividad. */
public record ActivityResponseDto(
        UUID id,
        UUID communityId,
        UUID createdBy,
        String title,
        String description,
        String location,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        ActivityStatus status,
        long participantCount,
        boolean isParticipating
) {}
