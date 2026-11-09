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
import com.musa.communities_service.enums.CommunityType;
import com.musa.communities_service.enums.CommunityVisibility;
import com.musa.communities_service.enums.MemberRole;
import com.musa.communities_service.enums.MemberStatus;
import com.musa.communities_service.enums.ParticipantStatus;
import com.musa.communities_service.exception.BusinessRuleException;
import com.musa.communities_service.exception.ForbiddenActionException;
import com.musa.communities_service.mapper.ActivityMapper;
import com.musa.communities_service.repository.ActivityParticipantRepository;
import com.musa.communities_service.repository.CommunityActivityRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para validar la lógica de negocio en CommunityActivityServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class CommunityActivityServiceImplTest {

    @Mock
    private CommunityRepository communityRepository;

    @Mock
    private CommunityActivityRepository activityRepository;

    @Mock
    private ActivityParticipantRepository participantRepository;

    @Mock
    private CommunityMemberRepository memberRepository;

    @Mock
    private ActivityMapper activityMapper;

    @InjectMocks
    private CommunityActivityServiceImpl activityService;

    private UUID communityId;
    private UUID activityId;
    private UUID userId;
    private Community community;
    private CommunityActivity activity;
    private ActivityResponseDto activityResponseDto;

    @BeforeEach
    void setUp() {
        communityId = UUID.randomUUID();
        activityId = UUID.randomUUID();
        userId = UUID.randomUUID();

        community = new Community("Club de Lectura", "Descripción", CommunityType.LIBROS, CommunityVisibility.PUBLICA, userId);
        community.setId(communityId);

        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        OffsetDateTime end = start.plusHours(2);

        activity = new CommunityActivity(community, userId, "Debate de Libro", "Discusión", "Zoom", start, end, ActivityStatus.PROGRAMADA);
        activity.setId(activityId);

        activityResponseDto = new ActivityResponseDto(
                activityId, communityId, userId, "Debate de Libro", "Discusión", "Zoom", start, end,
                OffsetDateTime.now(), OffsetDateTime.now(), ActivityStatus.PROGRAMADA, 0L, false
        );
    }

    // PRUEBAS PARA: createActivity (CU-49)
    @Test
    @DisplayName("CU-49: Debe crear una actividad correctamente cuando el usuario es ADMINISTRADOR")
    void createActivity_debeCrearActividad_cuandoEsAdmin() {
        // ARRANGE: preparar solicitud y validar permisos de administración
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        OffsetDateTime end = start.plusHours(2);
        ActivityCreateRequestDto request = new ActivityCreateRequestDto("Debate de Libro", "Discusión", "Zoom", start, end);

        when(communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)).thenReturn(Optional.of(community));
        when(memberRepository.existsByCommunityIdAndUserIdAndRoleAndStatus(communityId, userId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO)).thenReturn(true);
        when(activityMapper.toEntity(request, community, userId)).thenReturn(activity);
        when(activityRepository.save(activity)).thenReturn(activity);
        when(activityMapper.toResponseDto(activity, 0L, false)).thenReturn(activityResponseDto);

        // ACT: invocar la creación de la actividad
        ActivityResponseDto result = activityService.createActivity(communityId, request, userId);

        // ASSERT: comprobar la respuesta obtenida y la ejecución del guardado
        assertNotNull(result);
        assertEquals("Debate de Libro", result.title());
        verify(activityRepository).save(activity);
    }

    @Test
    @DisplayName("CU-49: Debe lanzar ForbiddenActionException si un usuario regular intenta crear una actividad")
    void createActivity_debeLanzarExcepcion_siNoEsAdmin() {
        // ARRANGE: simular usuario que no es administrador de la comunidad
        ActivityCreateRequestDto request = new ActivityCreateRequestDto("Debate", "Desc", "Zoom", OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(1));
        when(communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)).thenReturn(Optional.of(community));
        when(memberRepository.existsByCommunityIdAndUserIdAndRoleAndStatus(communityId, userId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO)).thenReturn(false);

        // ACT & ASSERT: verificar el rechazo por falta de permisos
        assertThrows(ForbiddenActionException.class, () -> activityService.createActivity(communityId, request, userId));
        verify(activityRepository, never()).save(any());
    }

    @Test
    @DisplayName("CU-49: Debe lanzar BusinessRuleException si fecha final es anterior a la fecha inicial")
    void createActivity_debeLanzarExcepcion_siFechasSonInvalidas() {
        // ARRANGE: preparar fechas inconsistentes (fin antes de inicio)
        OffsetDateTime start = OffsetDateTime.now().plusDays(2);
        OffsetDateTime end = OffsetDateTime.now().plusDays(1);
        ActivityCreateRequestDto request = new ActivityCreateRequestDto("Debate", "Desc", "Zoom", start, end);

        when(communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)).thenReturn(Optional.of(community));
        when(memberRepository.existsByCommunityIdAndUserIdAndRoleAndStatus(communityId, userId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO)).thenReturn(true);

        // ACT & ASSERT: verificar validación de fechas de negocio
        assertThrows(BusinessRuleException.class, () -> activityService.createActivity(communityId, request, userId));
    }

    // PRUEBAS PARA: updateActivity (CU-50)
    @Test
    @DisplayName("CU-50: Debe actualizar la actividad correctamente")
    void updateActivity_debeActualizarActividadExitosamente() {
        // ARRANGE: simular búsqueda exitosa de actividad programada y actualización
        ActivityUpdateRequestDto request = new ActivityUpdateRequestDto("Título Nuevo", null, null, null, null);
        when(communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)).thenReturn(Optional.of(community));
        when(memberRepository.existsByCommunityIdAndUserIdAndRoleAndStatus(communityId, userId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO)).thenReturn(true);
        when(activityRepository.findByIdAndCommunityIdAndStatus(activityId, communityId, ActivityStatus.PROGRAMADA)).thenReturn(Optional.of(activity));
        when(activityRepository.save(activity)).thenReturn(activity);
        when(participantRepository.countByActivityIdAndStatus(activityId, ParticipantStatus.CONFIRMADA)).thenReturn(3L);
        when(participantRepository.existsByActivityIdAndUserIdAndStatus(activityId, userId, ParticipantStatus.CONFIRMADA)).thenReturn(true);
        when(activityMapper.toResponseDto(activity, 3L, true)).thenReturn(activityResponseDto);

        // ACT: ejecutar actualización de la actividad
        ActivityResponseDto result = activityService.updateActivity(communityId, activityId, request, userId);

        // ASSERT: comprobar la invocación del guardado
        assertNotNull(result);
        verify(activityRepository).save(activity);
    }

    // PRUEBAS PARA: deleteActivity (CU-51)
    @Test
    @DisplayName("CU-51: Debe cancelar una actividad cambiando su estado a CANCELADA")
    void deleteActivity_debeCancelarActividad() {
        // ARRANGE: simular búsqueda de la actividad a cancelar por parte del administrador
        when(communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)).thenReturn(Optional.of(community));
        when(memberRepository.existsByCommunityIdAndUserIdAndRoleAndStatus(communityId, userId, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO)).thenReturn(true);
        when(activityRepository.findByIdAndCommunityIdAndStatus(activityId, communityId, ActivityStatus.PROGRAMADA)).thenReturn(Optional.of(activity));

        // ACT: ejecutar la eliminación/cancelación
        activityService.deleteActivity(communityId, activityId, userId);

        // ASSERT: comprobar la actualización del estado a CANCELADA
        assertEquals(ActivityStatus.CANCELADA, activity.getStatus());
        verify(activityRepository).save(activity);
    }

    // PRUEBAS PARA: getCommunityActivities (CU-52)
    @Test
    @DisplayName("CU-52: Debe retornar las actividades programadas paginadas para una comunidad")
    void getCommunityActivities_debeRetornarPaginaDeActividades() {
        // ARRANGE: simular la consulta paginada de actividades programadas
        Pageable pageable = PageRequest.of(0, 10);
        Page<CommunityActivity> page = new PageImpl<>(List.of(activity));

        when(communityRepository.findByIdAndStatus(communityId, CommunityStatus.ACTIVA)).thenReturn(Optional.of(community));
        when(activityRepository.findByCommunityIdAndStatus(communityId, ActivityStatus.PROGRAMADA, pageable)).thenReturn(page);
        when(participantRepository.countByActivityIdAndStatus(activityId, ParticipantStatus.CONFIRMADA)).thenReturn(5L);
        when(participantRepository.existsByActivityIdAndUserIdAndStatus(activityId, userId, ParticipantStatus.CONFIRMADA)).thenReturn(false);
        when(activityMapper.toResponseDto(activity, 5L, false)).thenReturn(activityResponseDto);

        // ACT: consultar actividades de la comunidad
        Page<ActivityResponseDto> result = activityService.getCommunityActivities(communityId, pageable, userId);

        // ASSERT: validar el contenido de la página retornada
        assertAll(
                () -> assertEquals(1, result.getTotalElements()),
                () -> assertEquals("Debate de Libro", result.getContent().get(0).title())
        );
    }

    // PRUEBAS PARA: confirmAttendance (CU-53)
    @Test
    @DisplayName("CU-53: Debe registrar o actualizar la asistencia (RSVP) a una actividad si es miembro de la comunidad")
    void confirmAttendance_debeConfirmarAsistencia_siEsMiembro() {
        // ARRANGE: simular que el usuario es miembro activo de la comunidad organizadora
        ActivityParticipantResponseDto participantDto = new ActivityParticipantResponseDto(UUID.randomUUID(), activityId, userId, ParticipantStatus.CONFIRMADA, OffsetDateTime.now());

        when(activityRepository.findByIdAndStatus(activityId, ActivityStatus.PROGRAMADA)).thenReturn(Optional.of(activity));
        when(memberRepository.existsByCommunityIdAndUserIdAndStatus(communityId, userId, MemberStatus.ACTIVO)).thenReturn(true);
        when(participantRepository.findByActivityIdAndUserId(activityId, userId)).thenReturn(Optional.empty());
        when(participantRepository.save(any(ActivityParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityMapper.toParticipantResponseDto(any(ActivityParticipant.class))).thenReturn(participantDto);

        // ACT: confirmar asistencia a la actividad
        ActivityParticipantResponseDto result = activityService.confirmAttendance(activityId, userId);

        // ASSERT: comprobar que la asistencia haya sido grabada con estado CONFIRMADA
        assertNotNull(result);
        assertEquals(ParticipantStatus.CONFIRMADA, result.status());
        verify(participantRepository).save(any(ActivityParticipant.class));
    }

    @Test
    @DisplayName("CU-53: Debe lanzar BusinessRuleException si un usuario que no es miembro intenta confirmar asistencia")
    void confirmAttendance_debeLanzarExcepcion_siNoEsMiembroDeLaComunidad() {
        // ARRANGE: usuario sin membresía activa en la comunidad
        when(activityRepository.findByIdAndStatus(activityId, ActivityStatus.PROGRAMADA)).thenReturn(Optional.of(activity));
        when(memberRepository.existsByCommunityIdAndUserIdAndStatus(communityId, userId, MemberStatus.ACTIVO)).thenReturn(false);

        // ACT & ASSERT: comprobar la restricción que exige pertenecer a la comunidad previa asistencia
        assertThrows(BusinessRuleException.class, () -> activityService.confirmAttendance(activityId, userId));
        verify(participantRepository, never()).save(any());
    }
}
