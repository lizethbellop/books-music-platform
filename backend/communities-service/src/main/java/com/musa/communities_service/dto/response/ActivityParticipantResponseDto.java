package com.musa.communities_service.dto.response;

import com.musa.communities_service.enums.ParticipantStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** DTO de respuesta para representar la confirmación o participación de un usuario en una actividad. */
public record ActivityParticipantResponseDto(
        UUID id,
        UUID activityId,
        UUID userId,
        ParticipantStatus status,
        OffsetDateTime confirmedAt
) {}