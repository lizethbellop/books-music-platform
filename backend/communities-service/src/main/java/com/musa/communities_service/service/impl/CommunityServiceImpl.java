package com.musa.communities_service.service.impl;

import com.musa.communities_service.dto.request.CommunityCreateRequestDto;
import com.musa.communities_service.dto.request.CommunityUpdateRequestDto;
import com.musa.communities_service.dto.response.CommunityMemberResponseDto;
import com.musa.communities_service.dto.response.CommunityResponseDto;
import com.musa.communities_service.entity.Community;
import com.musa.communities_service.entity.CommunityMember;
import com.musa.communities_service.enums.CommunityStatus;
import com.musa.communities_service.enums.CommunityType;
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

        return communityMapper.toResponseDto(savedCommunity, 1L, true, MemberRole.ADMINISTRADOR);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommunityResponseDto> searchCommunities(
            String query, CommunityType type, Pageable pageable, UUID currentUserId) {

        Page<Community> communities = communityRepository.searchCommunities(query, type, CommunityStatus.ACTIVA, pageable);

        return communities.map(community -> {
            long memberCount = memberRepository.countByCommunityIdAndStatus(community.getId(), MemberStatus.ACTIVO);

            Optional<CommunityMember> memberOpt = memberRepository
                    .findByCommunityIdAndUserIdAndStatus(community.getId(), currentUserId, MemberStatus.ACTIVO);

            boolean isMember = memberOpt.isPresent();
            MemberRole userRole = memberOpt.map(CommunityMember::getRole).orElse(null);

            return communityMapper.toResponseDto(community, memberCount, isMember, userRole);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityResponseDto getCommunityById(UUID communityId, UUID currentUserId) {
        Community community = communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)
                .orElseThrow(() -> new ResourceNotFoundException("Comunidad no encontrada con el id: " + communityId));

        long memberCount = memberRepository.countByCommunityIdAndStatus(communityId, MemberStatus.ACTIVO);

        Optional<CommunityMember> memberOpt = memberRepository
                .findByCommunityIdAndUserIdAndStatus(communityId, currentUserId, MemberStatus.ACTIVO);

        boolean isMember = memberOpt.isPresent();
        MemberRole userRole = memberOpt.map(CommunityMember::getRole).orElse(null);

        return communityMapper.toResponseDto(community, memberCount, isMember, userRole);
    }

    @Override
    @Transactional
    public CommunityResponseDto updateCommunity(UUID communityId, CommunityUpdateRequestDto request, UUID currentUserId) {
        Community community = communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)
                .orElseThrow(() -> new ResourceNotFoundException("Comunidad no encontrada con el id: " + communityId));

        validateAdminRole(communityId, currentUserId);

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
        long memberCount = memberRepository.countByCommunityIdAndStatus(communityId, MemberStatus.ACTIVO);

        return communityMapper.toResponseDto(updatedCommunity, memberCount, true, MemberRole.ADMINISTRADOR);
    }

    @Override
    @Transactional
    public CommunityMemberResponseDto joinCommunity(UUID communityId, UUID currentUserId) {
        Community community = communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)
                .orElseThrow(() -> new ResourceNotFoundException("Comunidad no encontrada con el id: " + communityId));

        Optional<CommunityMember> existingMemberOpt = memberRepository
                .findByCommunityIdAndUserId(communityId, currentUserId);

        if (existingMemberOpt.isPresent()) {
            CommunityMember existingMember = existingMemberOpt.get();
            if (existingMember.getStatus() == MemberStatus.ACTIVO) {
                throw new BusinessRuleException("Ya eres miembro activo de esta comunidad");
            }
            existingMember.setStatus(MemberStatus.ACTIVO);
            existingMember.setJoinedAt(OffsetDateTime.now(ZoneOffset.UTC));
            return communityMapper.toMemberResponseDto(memberRepository.save(existingMember));
        }

        CommunityMember newMember = new CommunityMember(
                community,
                currentUserId,
                MemberRole.MIEMBRO,
                MemberStatus.ACTIVO
        );

        return communityMapper.toMemberResponseDto(memberRepository.save(newMember));
    }

    @Override
    @Transactional
    public void leaveCommunity(UUID communityId, UUID currentUserId) {
        CommunityMember member = memberRepository
                .findByCommunityIdAndUserIdAndStatus(communityId, currentUserId, MemberStatus.ACTIVO)
                .orElseThrow(() -> new BusinessRuleException("No eres un miembro activo de esta comunidad"));

        if (member.getRole() == MemberRole.ADMINISTRADOR) {
            long activeAdmins = memberRepository.countByCommunityIdAndRoleAndStatus(
                    communityId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO);
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

    private void validateAdminRole(UUID communityId, UUID userId) {
        boolean isAdmin = memberRepository.existsByCommunityIdAndUserIdAndRoleAndStatus(
                communityId, userId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO);
        if (!isAdmin) {
            throw new ForbiddenActionException("Se requieren permisos de ADMINISTRADOR para realizar esta acción");
        }
    }
}
