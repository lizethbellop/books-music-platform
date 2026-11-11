package com.musa.communities_service.mapper;

import com.musa.communities_service.dto.request.CommunityCreateRequestDto;
import com.musa.communities_service.dto.response.CommunityMemberResponseDto;
import com.musa.communities_service.dto.response.CommunityResponseDto;
import com.musa.communities_service.entity.Community;
import com.musa.communities_service.entity.CommunityMember;
import com.musa.communities_service.enums.MemberRole;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Componente convertidor para la entidad Community y sus DTOs asociados.
 */
@Component
public class CommunityMapper {

    public Community toEntity(CommunityCreateRequestDto dto, UUID createdBy) {
        return new Community(
                dto.name(),
                dto.description(),
                dto.type(),
                dto.visibility(),
                createdBy
        );
    }

    public CommunityResponseDto toResponseDto(
            Community entity,
            long memberCount,
            boolean isMember,
            MemberRole userRole) {
        return new CommunityResponseDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getType(),
                entity.getVisibility(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getStatus(),
                memberCount,
                isMember,
                userRole
        );
    }

    public CommunityMemberResponseDto toMemberResponseDto(CommunityMember member) {
        return new CommunityMemberResponseDto(
                member.getId(),
                member.getCommunity().getId(),
                member.getUserId(),
                member.getRole(),
                member.getJoinedAt(),
                member.getStatus()
        );
    }
}