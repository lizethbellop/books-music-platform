package com.musa.communities_service.mapper;

import com.musa.communities_service.dto.request.ActivityCreateRequestDto;
import com.musa.communities_service.dto.response.ActivityParticipantResponseDto;
import com.musa.communities_service.dto.response.ActivityResponseDto;
import com.musa.communities_service.entity.ActivityParticipant;
import com.musa.communities_service.entity.Community;
import com.musa.communities_service.entity.CommunityActivity;
import com.musa.communities_service.enums.ActivityStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Componente convertidor para la entidad CommunityActivity y sus DTOs asociados.
 */
@Component
public class ActivityMapper {

    public CommunityActivity toEntity(ActivityCreateRequestDto dto, Community community, UUID createdBy) {
        return new CommunityActivity(
                community,
                createdBy,
                dto.title(),
                dto.description(),
                dto.location(),
                dto.startsAt(),
                dto.endsAt(),
                ActivityStatus.PROGRAMADA
        );
    }

    public ActivityResponseDto toResponseDto(
            CommunityActivity entity,
            long participantCount,
            boolean isParticipating) {
        return new ActivityResponseDto(
                entity.getId(),
                entity.getCommunity() != null ? entity.getCommunity().getId() : null,
                entity.getCreatedBy(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getLocation(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getStatus(),
                participantCount,
                isParticipating
        );
    }

    public ActivityParticipantResponseDto toParticipantResponseDto(ActivityParticipant participant) {
        return new ActivityParticipantResponseDto(
                participant.getId(),
                participant.getActivity().getId(),
                participant.getUserId(),
                participant.getStatus(),
                participant.getConfirmedAt()
        );
    }
}