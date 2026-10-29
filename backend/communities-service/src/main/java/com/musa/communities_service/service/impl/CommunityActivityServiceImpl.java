package com.musa.communities_service.service.impl;

import com.musa.communities_service.dto.request.ActivityCreateRequestDto;
import com.musa.communities_service.dto.request.ActivityUpdateRequestDto;
import com.musa.communities_service.dto.response.ActivityParticipantResponseDto;
import com.musa.communities_service.dto.response.ActivityResponseDto;
import com.musa.communities_service.entity.ActivityParticipant;
import com.musa.communities_service.entity.Community;
import com.musa.communities_service.entity.CommunityActivity;
import com.musa.communities_service.enums.ActivityStatus;
import com.musa.communities_service.enums.CommunityStatus;
import com.musa.communities_service.enums.MemberRole;
import com.musa.communities_service.enums.MemberStatus;
import com.musa.communities_service.enums.ParticipantStatus;
import com.musa.communities_service.exception.BusinessRuleException;
import com.musa.communities_service.exception.ForbiddenActionException;
import com.musa.communities_service.exception.ResourceNotFoundException;
import com.musa.communities_service.mapper.ActivityMapper;
import com.musa.communities_service.repository.ActivityParticipantRepository;
import com.musa.communities_service.repository.CommunityActivityRepository;
import com.musa.communities_service.repository.CommunityMemberRepository;
import com.musa.communities_service.repository.CommunityRepository;
import com.musa.communities_service.service.CommunityActivityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación de la lógica de negocio para actividades y asistencias.
 * Cubre los casos de uso: CU-49, CU-50, CU-51, CU-52 y CU-53.
 */
@Service
public class CommunityActivityServiceImpl implements CommunityActivityService {

    private final CommunityRepository communityRepository;
    private final CommunityActivityRepository activityRepository;
    private final ActivityParticipantRepository participantRepository;
    private final CommunityMemberRepository memberRepository;
    private final ActivityMapper activityMapper;

    public CommunityActivityServiceImpl(
            CommunityRepository communityRepository,
            CommunityActivityRepository activityRepository,
            ActivityParticipantRepository participantRepository,
            CommunityMemberRepository memberRepository,
            ActivityMapper activityMapper) {
        this.communityRepository = communityRepository;
        this.activityRepository = activityRepository;
        this.participantRepository = participantRepository;
        this.memberRepository = memberRepository;
        this.activityMapper = activityMapper;
    }

    @Override
    @Transactional
    public ActivityResponseDto createActivity(UUID communityId, ActivityCreateRequestDto request, UUID currentUserId) {
        Community community = findActiveCommunityOrThrow(communityId);
        validateAdminRole(communityId, currentUserId);

        if (request.endsAt() != null && request.endsAt().isBefore(request.startsAt())) {
            throw new BusinessRuleException("La fecha de finalización no puede ser anterior a la fecha de inicio");
        }

        CommunityActivity activity = activityMapper.toEntity(request, community, currentUserId);
        CommunityActivity savedActivity = activityRepository.save(activity);

        return activityMapper.toResponseDto(savedActivity, 0L, false);
    }

    @Override
    @Transactional
    public ActivityResponseDto updateActivity(
            UUID communityId, UUID activityId, ActivityUpdateRequestDto request, UUID currentUserId) {
        findActiveCommunityOrThrow(communityId);
        validateAdminRole(communityId, currentUserId);

        CommunityActivity activity = activityRepository.findByIdAndCommunityIdAndStatus(
                        activityId, communityId, ActivityStatus.PROGRAMADA)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada o no está en estado PROGRAMADA"));

        if (request.title() != null) {
            activity.setTitle(request.title());
        }
        if (request.description() != null) {
            activity.setDescription(request.description());
        }
        if (request.location() != null) {
            activity.setLocation(request.location());
        }
        if (request.startsAt() != null) {
            activity.setStartsAt(request.startsAt());
        }
        if (request.endsAt() != null) {
            activity.setEndsAt(request.endsAt());
        }

        if (activity.getEndsAt() != null && activity.getEndsAt().isBefore(activity.getStartsAt())) {
            throw new BusinessRuleException("La fecha de finalización no puede ser anterior a la fecha de inicio");
        }

        CommunityActivity updatedActivity = activityRepository.save(activity);
        long participantCount = participantRepository.countByActivityIdAndStatus(activityId, ParticipantStatus.CONFIRMADA);
        boolean isParticipating = participantRepository.existsByActivityIdAndUserIdAndStatus(
                activityId, currentUserId, ParticipantStatus.CONFIRMADA);

        return activityMapper.toResponseDto(updatedActivity, participantCount, isParticipating);
    }

    @Override
    @Transactional
    public void deleteActivity(UUID communityId, UUID activityId, UUID currentUserId) {
        findActiveCommunityOrThrow(communityId);
        validateAdminRole(communityId, currentUserId);

        CommunityActivity activity = activityRepository.findByIdAndCommunityIdAndStatus(
                        activityId, communityId, ActivityStatus.PROGRAMADA)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada con el id: " + activityId));

        activity.setStatus(ActivityStatus.CANCELADA);
        activityRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityResponseDto> getCommunityActivities(UUID communityId, Pageable pageable, UUID currentUserId) {
        findActiveCommunityOrThrow(communityId);

        Page<CommunityActivity> activities = activityRepository
                .findByCommunityIdAndStatus(communityId, ActivityStatus.PROGRAMADA, pageable);

        return activities.map(activity -> {
            long participantCount = participantRepository.countByActivityIdAndStatus(
                    activity.getId(), ParticipantStatus.CONFIRMADA);
            boolean isParticipating = participantRepository.existsByActivityIdAndUserIdAndStatus(
                    activity.getId(), currentUserId, ParticipantStatus.CONFIRMADA);

            return activityMapper.toResponseDto(activity, participantCount, isParticipating);
        });
    }

    @Override
    @Transactional
    public ActivityParticipantResponseDto confirmAttendance(UUID activityId, UUID currentUserId) {
        CommunityActivity activity = activityRepository.findByIdAndStatus(activityId, ActivityStatus.PROGRAMADA)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad no encontrada con el id: " + activityId));

        boolean isMember = memberRepository.existsByCommunityIdAndUserIdAndStatus(
                activity.getCommunity().getId(), currentUserId, MemberStatus.ACTIVO);
        if (!isMember) {
            throw new BusinessRuleException("Debes ser miembro de la comunidad para confirmar asistencia a sus actividades");
        }

        Optional<ActivityParticipant> participantOpt = participantRepository
                .findByActivityIdAndUserId(activityId, currentUserId);

        if (participantOpt.isPresent()) {
            ActivityParticipant participant = participantOpt.get();
            participant.setStatus(ParticipantStatus.CONFIRMADA);
            participant.setConfirmedAt(OffsetDateTime.now(ZoneOffset.UTC));
            return activityMapper.toParticipantResponseDto(participantRepository.save(participant));
        }

        ActivityParticipant newParticipant = new ActivityParticipant(
                activity,
                currentUserId,
                ParticipantStatus.CONFIRMADA
        );

        return activityMapper.toParticipantResponseDto(participantRepository.save(newParticipant));
    }

    private Community findActiveCommunityOrThrow(UUID communityId) {
        return communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)
                .orElseThrow(() -> new ResourceNotFoundException("Comunidad no encontrada con el id: " + communityId));
    }

    private void validateAdminRole(UUID communityId, UUID userId) {
        boolean isAdmin = memberRepository.existsByCommunityIdAndUserIdAndRoleAndStatus(
                communityId, userId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO);
        if (!isAdmin) {
            throw new ForbiddenActionException("Se requieren permisos de ADMINISTRADOR para realizar esta acción");
        }
    }
}
