package com.musa.communities_service.dto.response;

import com.musa.communities_service.enums.MemberRole;
import com.musa.communities_service.enums.MemberStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** DTO de respuesta para representar la membresía de un usuario en una comunidad. */
public record CommunityMemberResponseDto(
        UUID id,
        UUID communityId,
        UUID userId,
        MemberRole role,
        OffsetDateTime joinedAt,
        MemberStatus status
) {}