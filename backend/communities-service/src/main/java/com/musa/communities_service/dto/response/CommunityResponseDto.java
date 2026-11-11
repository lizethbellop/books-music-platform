package com.musa.communities_service.dto.response;

import com.musa.communities_service.enums.CommunityStatus;
import com.musa.communities_service.enums.CommunityType;
import com.musa.communities_service.enums.CommunityVisibility;
import com.musa.communities_service.enums.MemberRole;

import java.time.OffsetDateTime;
import java.util.UUID;

/** DTO de respuesta con la información detallada de una comunidad. */
public record CommunityResponseDto(
        UUID id,
        String name,
        String description,
        CommunityType type,
        CommunityVisibility visibility,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        CommunityStatus status,
        long memberCount,
        boolean isMember,
        MemberRole userRole
) {}
