package com.musa.communities_service.service.impl;

import com.musa.communities_service.dto.request.CommunityCreateRequestDto;
import com.musa.communities_service.dto.request.CommunityUpdateRequestDto;
import com.musa.communities_service.dto.response.CommunityMemberResponseDto;
import com.musa.communities_service.dto.response.CommunityResponseDto;
import com.musa.communities_service.entity.Community;
import com.musa.communities_service.entity.CommunityMember;
import com.musa.communities_service.enums.CommunityStatus;
import com.musa.communities_service.enums.CommunityType;
import com.musa.communities_service.enums.CommunityVisibility;
import com.musa.communities_service.enums.MemberRole;
import com.musa.communities_service.enums.MemberStatus;
import com.musa.communities_service.exception.BusinessRuleException;
import com.musa.communities_service.exception.ForbiddenActionException;
import com.musa.communities_service.exception.ResourceNotFoundException;
import com.musa.communities_service.mapper.CommunityMapper;
import com.musa.communities_service.repository.CommunityMemberRepository;
import com.musa.communities_service.repository.CommunityRepository;
import com.musa.communities_service.service.CommunityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación de la lógica de negocio para la gestión de comunidades y membresías.
 * Cubre los casos de uso: CU-41, CU-42, CU-43, CU-44, CU-45 y CU-54.
 */
@Service
public class CommunityServiceImpl implements CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final CommunityMapper communityMapper;

    public CommunityServiceImpl(
            CommunityRepository communityRepository,
            CommunityMemberRepository memberRepository,
            CommunityMapper communityMapper) {
        this.communityRepository = communityRepository;
        this.memberRepository = memberRepository;
        this.communityMapper = communityMapper;
    }

    @Override
    @Transactional
    public CommunityResponseDto createCommunity(CommunityCreateRequestDto request, UUID currentUserId) {
        if (communityRepository.existsByNameIgnoreCaseAndStatus(request.name(), CommunityStatus.ACTIVA)) {
            throw new BusinessRuleException("Ya existe una comunidad activa con el nombre: " + request.name());
        }

        Community community = communityMapper.toEntity(request, currentUserId);
        Community savedCommunity = communityRepository.save(community);

        CommunityMember adminMember = new CommunityMember(
                savedCommunity,
                currentUserId,
                MemberRole.ADMINISTRADOR,
                MemberStatus.ACTIVO
        );
        memberRepository.save(adminMember);

        return communityMapper.toResponseDto(savedCommunity, 1L, true, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommunityResponseDto> searchCommunities(
            String query, CommunityType type, Pageable pageable, UUID currentUserId) {

        String sanitizedQuery = (query == null || query.isBlank()) ? "" : query.trim();

        Page<Community> communities = communityRepository.searchCommunities(
                sanitizedQuery, type, CommunityStatus.ACTIVA, pageable);

        return communities.map(community -> mapCommunityWithMembership(community, currentUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityResponseDto getCommunityById(UUID communityId, UUID currentUserId) {
        Community community = communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)
                .orElseThrow(() -> new ResourceNotFoundException("Comunidad no encontrada con el id: " + communityId));

        return mapCommunityWithMembership(community, currentUserId);
    }
    
    @Override
    @Transactional
    public CommunityResponseDto updateCommunity(UUID communityId, CommunityUpdateRequestDto request, UUID currentUserId) {
        Community community = communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)
                .orElseThrow(() -> new ResourceNotFoundException("Comunidad no encontrada con el id: " + communityId));

        validateAdminRole(communityId, currentUserId);

        CommunityVisibility previousVisibility = community.getVisibility();

        if (request.description() != null) {
            community.setDescription(request.description());
        }
        if (request.type() != null) {
            community.setType(request.type());
        }
        if (request.visibility() != null) {
            community.setVisibility(request.visibility());
        }

        Community updatedCommunity = communityRepository.save(community);

        if (previousVisibility == CommunityVisibility.PRIVADA &&
            updatedCommunity.getVisibility() == CommunityVisibility.PUBLICA) {
            List<CommunityMember> pendingMembers = memberRepository
                    .findByCommunityIdAndStatusOrderByJoinedAtAsc(communityId, MemberStatus.PENDIENTE);
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            for (CommunityMember pending : pendingMembers) {
                pending.setStatus(MemberStatus.ACTIVO);
                pending.setJoinedAt(now);
            }
            memberRepository.saveAll(pendingMembers);
        }

        long memberCount = memberRepository.countByCommunityIdAndStatus(communityId, MemberStatus.ACTIVO);

        return communityMapper.toResponseDto(
                updatedCommunity, memberCount, true, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO);
    }

    @Override
    @Transactional
    public CommunityMemberResponseDto joinCommunity(UUID communityId, UUID currentUserId) {
        Community community = communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)
                .orElseThrow(() -> new ResourceNotFoundException("Comunidad no encontrada con el id: " + communityId));

        MemberStatus targetStatus = (community.getVisibility() == CommunityVisibility.PRIVADA)
                ? MemberStatus.PENDIENTE
                : MemberStatus.ACTIVO;

        Optional<CommunityMember> existingMemberOpt = memberRepository
                .findByCommunityIdAndUserId(communityId, currentUserId);

        if (existingMemberOpt.isPresent()) {
            CommunityMember existingMember = existingMemberOpt.get();
            if (existingMember.getStatus() == MemberStatus.ACTIVO) {
                throw new BusinessRuleException("Ya eres miembro activo de esta comunidad");
            }
            if (existingMember.getStatus() == MemberStatus.PENDIENTE) {
                throw new BusinessRuleException("Ya tienes una solicitud pendiente para esta comunidad");
            }
            existingMember.setRole(MemberRole.MIEMBRO);
            existingMember.setStatus(targetStatus);
            existingMember.setJoinedAt(OffsetDateTime.now(ZoneOffset.UTC));
            return communityMapper.toMemberResponseDto(memberRepository.save(existingMember));
        }

        CommunityMember newMember = new CommunityMember(
                community,
                currentUserId,
                MemberRole.MIEMBRO,
                targetStatus
        );

        return communityMapper.toMemberResponseDto(memberRepository.save(newMember));
    }

    @Override
    @Transactional
    public void leaveCommunity(UUID communityId, UUID currentUserId) {
        CommunityMember member = memberRepository
                .findByCommunityIdAndUserId(communityId, currentUserId)
                .filter(m -> m.getStatus() == MemberStatus.ACTIVO || m.getStatus() == MemberStatus.PENDIENTE)
                .orElseThrow(() -> new BusinessRuleException("No eres miembro ni tienes una solicitud pendiente en esta comunidad"));

        if (member.getStatus() == MemberStatus.ACTIVO && member.getRole() == MemberRole.ADMINISTRADOR) {
            long activeAdmins = memberRepository.countByCommunityIdAndRoleAndStatus(communityId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO);
            if (activeAdmins <= 1) {
                throw new BusinessRuleException("No puedes salir de la comunidad siendo el único administrador");
            }
        }

        member.setStatus(MemberStatus.RETIRADO);
        memberRepository.save(member);
    }

    @Override
    @Transactional
    public void deleteCommunity(UUID communityId, UUID currentUserId) {
        Community community = communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)
                .orElseThrow(() -> new ResourceNotFoundException("Comunidad no encontrada con el id: " + communityId));

        validateAdminRole(communityId, currentUserId);

        community.setStatus(CommunityStatus.ELIMINADA);
        communityRepository.save(community);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunityMemberResponseDto> getCommunityMembers(UUID communityId, UUID currentUserId) {
        Community community = communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)
                .orElseThrow(() -> new ResourceNotFoundException("Comunidad no encontrada con el id: " + communityId));

        if (community.getVisibility() == CommunityVisibility.PRIVADA &&
            !memberRepository.existsByCommunityIdAndUserIdAndStatus(communityId, currentUserId, MemberStatus.ACTIVO)) {
            throw new ForbiddenActionException("Debes ser miembro activo para ver los integrantes de una comunidad privada");
        }

        List<CommunityMember> members = new ArrayList<>(
                memberRepository.findByCommunityIdAndStatusOrderByJoinedAtAsc(communityId, MemberStatus.ACTIVO)
        );

        boolean isAdmin = memberRepository.existsByCommunityIdAndUserIdAndRoleAndStatus(
                communityId, currentUserId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO);

        if (isAdmin) {
            members.addAll(
                    memberRepository.findByCommunityIdAndStatusOrderByJoinedAtAsc(communityId, MemberStatus.PENDIENTE)
            );
        }

        return members.stream()
                .map(communityMapper::toMemberResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public CommunityMemberResponseDto approveMember(UUID communityId, UUID targetUserId, UUID currentUserId) {
        communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)
                .orElseThrow(() -> new ResourceNotFoundException("Comunidad no encontrada con el id: " + communityId));

        validateAdminRole(communityId, currentUserId);

        CommunityMember pendingMember = memberRepository
                .findByCommunityIdAndUserIdAndStatus(communityId, targetUserId, MemberStatus.PENDIENTE)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una solicitud pendiente para este usuario"));

        pendingMember.setStatus(MemberStatus.ACTIVO);
        pendingMember.setJoinedAt(OffsetDateTime.now(ZoneOffset.UTC));

        return communityMapper.toMemberResponseDto(memberRepository.save(pendingMember));
    }

    @Override
    @Transactional
    public void rejectOrRemoveMember(UUID communityId, UUID targetUserId, UUID currentUserId) {
        communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)
                .orElseThrow(() -> new ResourceNotFoundException("Comunidad no encontrada con el id: " + communityId));

        validateAdminRole(communityId, currentUserId);

        if (targetUserId.equals(currentUserId)) {
            throw new BusinessRuleException("No puedes expulsarte a ti mismo; utiliza la opción de salir de la comunidad");
        }

        CommunityMember member = memberRepository
                .findByCommunityIdAndUserId(communityId, targetUserId)
                .filter(m -> m.getStatus() == MemberStatus.ACTIVO || m.getStatus() == MemberStatus.PENDIENTE)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no es miembro activo ni tiene solicitud pendiente"));

        member.setStatus(MemberStatus.RETIRADO);
        memberRepository.save(member);
    }

    private CommunityResponseDto mapCommunityWithMembership(Community community, UUID currentUserId) {
        long memberCount = memberRepository.countByCommunityIdAndStatus(community.getId(), MemberStatus.ACTIVO);

        Optional<CommunityMember> memberOpt = memberRepository
                .findByCommunityIdAndUserId(community.getId(), currentUserId)
                .filter(m -> m.getStatus() == MemberStatus.ACTIVO || m.getStatus() == MemberStatus.PENDIENTE);

        MemberStatus membershipStatus = memberOpt.map(CommunityMember::getStatus).orElse(null);
        boolean isMember = (membershipStatus == MemberStatus.ACTIVO);
        MemberRole userRole = isMember ? memberOpt.get().getRole() : null;

        return communityMapper.toResponseDto(community, memberCount, isMember, userRole, membershipStatus);
    }

    private void validateAdminRole(UUID communityId, UUID userId) {
        boolean isAdmin = memberRepository.existsByCommunityIdAndUserIdAndRoleAndStatus(
                communityId, userId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO);
        if (!isAdmin) {
            throw new ForbiddenActionException("Se requieren permisos de ADMINISTRADOR para realizar esta acción");
        }
    }
}
