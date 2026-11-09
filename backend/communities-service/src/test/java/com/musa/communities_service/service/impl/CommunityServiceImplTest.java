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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para validar la lógica de negocio en CommunityServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class CommunityServiceImplTest {

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private CommunityMemberRepository memberRepository;

    @Mock
    private CommunityMapper communityMapper;

    @InjectMocks
    private CommunityServiceImpl communityService;

    private UUID communityId;
    private UUID userId;
    private Community community;
    private CommunityResponseDto responseDto;

    @BeforeEach
    void setUp() {
        communityId = UUID.randomUUID();
        userId = UUID.randomUUID();

        community = new Community(
                "Club de Lectura Musa",
                "Comunidad literaria",
                CommunityType.LIBROS,
                CommunityVisibility.PUBLICA,
                userId
        );
        community.setId(communityId);

        responseDto = new CommunityResponseDto(
                communityId,
                "Club de Lectura Musa",
                "Comunidad literaria",
                CommunityType.LIBROS,
                CommunityVisibility.PUBLICA,
                userId,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                CommunityStatus.ACTIVA,
                1L,
                true,
                MemberRole.ADMINISTRADOR
        );
    }

    // PRUEBAS PARA: createCommunity (CU-41)
    @Test
    @DisplayName("CU-41: Debe crear una comunidad exitosamente y asignar al creador como ADMINISTRADOR")
    void createCommunity_debeCrearComunidadExitosamente() {
        // ARRANGE: preparar solicitud y configurar simulaciones de repositorio y mapper
        CommunityCreateRequestDto request = new CommunityCreateRequestDto(
                "Club de Lectura Musa", "Comunidad literaria", CommunityType.LIBROS, CommunityVisibility.PUBLICA
        );

        when(communityRepository.existsByNameIgnoreCaseAndStatus("Club de Lectura Musa", CommunityStatus.ACTIVA))
                .thenReturn(false);
        when(communityMapper.toEntity(request, userId)).thenReturn(community);
        when(communityRepository.save(community)).thenReturn(community);
        when(communityMapper.toResponseDto(community, 1L, true, MemberRole.ADMINISTRADOR)).thenReturn(responseDto);

        // ACT: ejecutar la creación de la comunidad
        CommunityResponseDto result = communityService.createCommunity(request, userId);

        // ASSERT: comprobar respuesta y verificar el guardado de la comunidad y del miembro administrador
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals("Club de Lectura Musa", result.name()),
                () -> assertEquals(MemberRole.ADMINISTRADOR, result.userRole())
        );

        verify(communityRepository).save(community);
        verify(memberRepository).save(any(CommunityMember.class));
    }

    @Test
    @DisplayName("CU-41: Debe lanzar BusinessRuleException al intentar crear una comunidad con nombre existente")
    void createCommunity_debeLanzarExcepcion_siNombreYaExiste() {
        // ARRANGE: simular que el nombre ya está registrado en el repositorio
        CommunityCreateRequestDto request = new CommunityCreateRequestDto(
                "Club de Lectura Musa", "Comunidad literaria", CommunityType.LIBROS, CommunityVisibility.PUBLICA
        );

        when(communityRepository.existsByNameIgnoreCaseAndStatus("Club de Lectura Musa", CommunityStatus.ACTIVA))
                .thenReturn(true);

        // ACT & ASSERT: verificar la excepción por nombre duplicado y que no se persistan registros
        assertThrows(BusinessRuleException.class, () -> communityService.createCommunity(request, userId));
        verify(communityRepository, never()).save(any());
        verify(memberRepository, never()).save(any());
    }

    // PRUEBAS PARA: searchCommunities (CU-42)
    @Test
    @DisplayName("CU-42: Debe realizar la búsqueda paginada mapeando contadores y estado del usuario")
    void searchCommunities_debeRetornarPaginaMapeadaConMetadatos() {
        // ARRANGE: preparar la página de entidades devueltas por la consulta JPQL
        Pageable pageable = PageRequest.of(0, 10);
        Page<Community> communityPage = new PageImpl<>(List.of(community));
        CommunityMember member = new CommunityMember(community, userId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO);

        when(communityRepository.searchCommunities("Lectura", CommunityType.LIBROS, CommunityStatus.ACTIVA, pageable))
                .thenReturn(communityPage);
        when(memberRepository.countByCommunityIdAndStatus(communityId, MemberStatus.ACTIVO)).thenReturn(5L);
        when(memberRepository.findByCommunityIdAndUserIdAndStatus(communityId, userId, MemberStatus.ACTIVO))
                .thenReturn(Optional.of(member));
        when(communityMapper.toResponseDto(community, 5L, true, MemberRole.ADMINISTRADOR)).thenReturn(responseDto);

        // ACT: ejecutar la búsqueda de comunidades
        Page<CommunityResponseDto> result = communityService.searchCommunities("Lectura", CommunityType.LIBROS, pageable, userId);

        // ASSERT: comprobar los elementos recuperados y las transformaciones aplicadas
        assertAll(
                () -> assertEquals(1, result.getTotalElements()),
                () -> assertEquals("Club de Lectura Musa", result.getContent().get(0).name())
        );
    }

    // PRUEBAS PARA: getCommunityById (CU-43)
    @Test
    @DisplayName("CU-43: Debe retornar el detalle de la comunidad cuando existe")
    void getCommunityById_debeRetornarComunidad_cuandoExiste() {
        // ARRANGE: configurar el hallazgo de la comunidad activa y sus membresías
        when(communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)).thenReturn(Optional.of(community));
        when(memberRepository.countByCommunityIdAndStatus(communityId, MemberStatus.ACTIVO)).thenReturn(10L);
        when(memberRepository.findByCommunityIdAndUserIdAndStatus(communityId, userId, MemberStatus.ACTIVO)).thenReturn(Optional.empty());
        when(communityMapper.toResponseDto(community, 10L, false, null)).thenReturn(responseDto);

        // ACT: consultar por identificador
        CommunityResponseDto result = communityService.getCommunityById(communityId, userId);

        // ASSERT: validar los datos retornados
        assertNotNull(result);
        verify(communityRepository).findByIdAndStatus(communityId, CommunityStatus.ACTIVA);
    }

    @Test
    @DisplayName("CU-43: Debe lanzar ResourceNotFoundException si la comunidad no existe")
    void getCommunityById_debeLanzarExcepcion_siNoExiste() {
        // ARRANGE: simular que el repositorio no localiza la comunidad
        when(communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)).thenReturn(Optional.empty());

        // ACT & ASSERT: verificar la excepción de recurso no encontrado
        assertThrows(ResourceNotFoundException.class, () -> communityService.getCommunityById(communityId, userId));
    }

    // PRUEBAS PARA: updateCommunity
    @Test
    @DisplayName("Debe actualizar la comunidad correctamente cuando el usuario es ADMINISTRADOR")
    void updateCommunity_debeActualizar_cuandoEsAdmin() {
        // ARRANGE: simular permisos de administrador y edición de datos
        CommunityUpdateRequestDto request = new CommunityUpdateRequestDto("Nueva descripción", CommunityType.MIXTO, CommunityVisibility.PUBLICA);
        when(communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)).thenReturn(Optional.of(community));
        when(memberRepository.existsByCommunityIdAndUserIdAndRoleAndStatus(communityId, userId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO)).thenReturn(true);
        when(communityRepository.save(community)).thenReturn(community);
        when(memberRepository.countByCommunityIdAndStatus(communityId, MemberStatus.ACTIVO)).thenReturn(1L);
        when(communityMapper.toResponseDto(community, 1L, true, MemberRole.ADMINISTRADOR)).thenReturn(responseDto);

        // ACT: invocar la actualización
        CommunityResponseDto result = communityService.updateCommunity(communityId, request, userId);

        // ASSERT: verificar el resultado y que la persistencia fue ejecutada
        assertNotNull(result);
        verify(communityRepository).save(community);
    }

    @Test
    @DisplayName("Debe lanzar ForbiddenActionException si un usuario sin rol ADMINISTRADOR intenta actualizar")
    void updateCommunity_debeLanzarExcepcion_siNoEsAdmin() {
        // ARRANGE: simular usuario sin permisos de administrador
        CommunityUpdateRequestDto request = new CommunityUpdateRequestDto("Nueva descripción", null, null);
        when(communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)).thenReturn(Optional.of(community));
        when(memberRepository.existsByCommunityIdAndUserIdAndRoleAndStatus(communityId, userId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO)).thenReturn(false);

        // ACT & ASSERT: comprobar el rechazo por permisos insuficientes
        assertThrows(ForbiddenActionException.class, () -> communityService.updateCommunity(communityId, request, userId));
        verify(communityRepository, never()).save(any());
    }

    // PRUEBAS PARA: joinCommunity (CU-44)
    @Test
    @DisplayName("CU-44: Debe registrar un nuevo miembro cuando no existía membresía previa")
    void joinCommunity_debeCrearNuevaMembresia() {
        // ARRANGE: simular ausencia de membresías previas
        CommunityMemberResponseDto memberDto = new CommunityMemberResponseDto(UUID.randomUUID(), communityId, userId, MemberRole.MIEMBRO, OffsetDateTime.now(), MemberStatus.ACTIVO);
        when(communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)).thenReturn(Optional.of(community));
        when(memberRepository.findByCommunityIdAndUserId(communityId, userId)).thenReturn(Optional.empty());
        when(memberRepository.save(any(CommunityMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(communityMapper.toMemberResponseDto(any(CommunityMember.class))).thenReturn(memberDto);

        // ACT: ejecutar la adhesión a la comunidad
        CommunityMemberResponseDto result = communityService.joinCommunity(communityId, userId);

        // ASSERT: comprobar la creación exitosa del nuevo miembro
        assertNotNull(result);
        assertEquals(MemberRole.MIEMBRO, result.role());
        verify(memberRepository).save(any(CommunityMember.class));
    }

    @Test
    @DisplayName("CU-44: Debe reactivar la membresía si el usuario figuraba como RETIRADO")
    void joinCommunity_debeReactivarMembresia_siEstabaRetirado() {
        // ARRANGE: instanciar miembro en estado RETIRADO
        CommunityMember retiredMember = new CommunityMember(community, userId, MemberRole.MIEMBRO, MemberStatus.RETIRADO);
        CommunityMemberResponseDto memberDto = new CommunityMemberResponseDto(UUID.randomUUID(), communityId, userId, MemberRole.MIEMBRO, OffsetDateTime.now(), MemberStatus.ACTIVO);

        when(communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)).thenReturn(Optional.of(community));
        when(memberRepository.findByCommunityIdAndUserId(communityId, userId)).thenReturn(Optional.of(retiredMember));
        when(memberRepository.save(retiredMember)).thenReturn(retiredMember);
        when(communityMapper.toMemberResponseDto(retiredMember)).thenReturn(memberDto);

        // ACT: solicitar unirse nuevamente
        CommunityMemberResponseDto result = communityService.joinCommunity(communityId, userId);

        // ASSERT: validar el cambio de estado a ACTIVO
        assertNotNull(result);
        assertEquals(MemberStatus.ACTIVO, retiredMember.getStatus());
        verify(memberRepository).save(retiredMember);
    }

    @Test
    @DisplayName("CU-44: Debe lanzar BusinessRuleException si el usuario ya es miembro activo")
    void joinCommunity_debeLanzarExcepcion_siYaEsMiembroActivo() {
        // ARRANGE: simular miembro en estado ACTIVO
        CommunityMember activeMember = new CommunityMember(community, userId, MemberRole.MIEMBRO, MemberStatus.ACTIVO);
        when(communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)).thenReturn(Optional.of(community));
        when(memberRepository.findByCommunityIdAndUserId(communityId, userId)).thenReturn(Optional.of(activeMember));

        // ACT & ASSERT: rechazar adhesión duplicada
        assertThrows(BusinessRuleException.class, () -> communityService.joinCommunity(communityId, userId));
    }

    // PRUEBAS PARA: leaveCommunity (CU-45)
    @Test
    @DisplayName("CU-45: Debe permitir la salida a un usuario con rol MIEMBRO")
    void leaveCommunity_debePermitirSalidaAMiembroRegular() {
        // ARRANGE: instanciar miembro activo regular
        CommunityMember member = new CommunityMember(community, userId, MemberRole.MIEMBRO, MemberStatus.ACTIVO);
        when(memberRepository.findByCommunityIdAndUserIdAndStatus(communityId, userId, MemberStatus.ACTIVO))
                .thenReturn(Optional.of(member));

        // ACT: procesar la salida voluntaria
        communityService.leaveCommunity(communityId, userId);

        // ASSERT: comprobar actualización de estado a RETIRADO
        assertEquals(MemberStatus.RETIRADO, member.getStatus());
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("CU-45: Debe lanzar BusinessRuleException si el único ADMINISTRADOR intenta salir")
    void leaveCommunity_debeLanzarExcepcion_siEsUnicoAdmin() {
        // ARRANGE: miembro administrador único activo
        CommunityMember adminMember = new CommunityMember(community, userId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO);
        when(memberRepository.findByCommunityIdAndUserIdAndStatus(communityId, userId, MemberStatus.ACTIVO))
                .thenReturn(Optional.of(adminMember));
        when(memberRepository.countByCommunityIdAndRoleAndStatus(communityId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO))
                .thenReturn(1L);

        // ACT & ASSERT: comprobar la protección que impide dejar comunidades huérfanas
        assertThrows(BusinessRuleException.class, () -> communityService.leaveCommunity(communityId, userId));
        verify(memberRepository, never()).save(any());
    }

    // PRUEBAS PARA: deleteCommunity (CU-54)
    @Test
    @DisplayName("CU-54: Debe realizar el borrado lógico de la comunidad cuando el usuario es ADMINISTRADOR")
    void deleteCommunity_debeRealizarBorradoLogico_cuandoEsAdmin() {
        // ARRANGE: simular búsqueda activa y permisos válidos de administrador
        when(communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)).thenReturn(Optional.of(community));
        when(memberRepository.existsByCommunityIdAndUserIdAndRoleAndStatus(communityId, userId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO)).thenReturn(true);

        // ACT: ejecutar la eliminación
        communityService.deleteCommunity(communityId, userId);

        // ASSERT: verificar el cambio de estado a ELIMINADA y su persistencia
        assertEquals(CommunityStatus.ELIMINADA, community.getStatus());
        verify(communityRepository).save(community);
    }
}