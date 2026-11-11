package com.musa.communities_service.controller;

import com.musa.communities_service.dto.request.ActivityCreateRequestDto;
import com.musa.communities_service.dto.request.ActivityUpdateRequestDto;
import com.musa.communities_service.dto.response.ActivityParticipantResponseDto;
import com.musa.communities_service.dto.response.ActivityResponseDto;
import com.musa.communities_service.service.CommunityActivityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para la gestión de actividades comunitarias y confirmación de asistencias.
 * Módulo de entrada para los casos de uso: CU-49, CU-50, CU-51, CU-52 y CU-53.
 */
@RestController
@RequestMapping("/api/v1")
public class CommunityActivityController {

    private final CommunityActivityService activityService;

    public CommunityActivityController(CommunityActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * Programa una nueva actividad dentro del contexto de una comunidad.
     * Caso de Uso: CU-49.
     *
     * @param communityId UUID de la comunidad organizadora.
     * @param request Datos del formulario de la actividad.
     * @param currentUserId UUID del usuario autenticado (debe ser ADMINISTRADOR).
     * @return Actividad creada y estado HTTP 201 (Created).
     */
    @PostMapping("/communities/{communityId}/activities")
    public ResponseEntity<ActivityResponseDto> createActivity(
            @PathVariable UUID communityId,
            @Valid @RequestBody ActivityCreateRequestDto request,
            @RequestHeader("X-User-Id") UUID currentUserId) {
        ActivityResponseDto response = activityService.createActivity(communityId, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Modifica los datos de una actividad previamente programada.
     * Caso de Uso: CU-50.
     *
     * @param communityId UUID de la comunidad organizadora.
     * @param activityId UUID de la actividad.
     * @param request Datos de actualización.
     * @param currentUserId UUID del usuario autenticado (debe ser ADMINISTRADOR).
     * @return Actividad modificada y estado HTTP 200 (OK).
     */
    @PutMapping("/communities/{communityId}/activities/{activityId}")
    public ResponseEntity<ActivityResponseDto> updateActivity(
            @PathVariable UUID communityId,
            @PathVariable UUID activityId,
            @Valid @RequestBody ActivityUpdateRequestDto request,
            @RequestHeader("X-User-Id") UUID currentUserId) {
        ActivityResponseDto response = activityService.updateActivity(communityId, activityId, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancela una actividad programada.
     * Caso de Uso: CU-51.
     *
     * @param communityId UUID de la comunidad.
     * @param activityId UUID de la actividad a cancelar.
     * @param currentUserId UUID del usuario autenticado (debe ser ADMINISTRADOR).
     * @return Estado HTTP 204 (No Content).
     */
    @DeleteMapping("/communities/{communityId}/activities/{activityId}")
    public ResponseEntity<Void> deleteActivity(
            @PathVariable UUID communityId,
            @PathVariable UUID activityId,
            @RequestHeader("X-User-Id") UUID currentUserId) {
        activityService.deleteActivity(communityId, activityId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Consulta las actividades programadas para una comunidad específica de forma paginada.
     * Caso de Uso: CU-52.
     *
     * @param communityId UUID de la comunidad.
     * @param pageable Configuración de paginación.
     * @param currentUserId UUID del usuario autenticado.
     * @return Página de actividades y estado HTTP 200 (OK).
     */
    @GetMapping("/communities/{communityId}/activities")
    public ResponseEntity<Page<ActivityResponseDto>> getCommunityActivities(
            @PathVariable UUID communityId,
            @PageableDefault(size = 10, sort = "startsAt") Pageable pageable,
            @RequestHeader("X-User-Id") UUID currentUserId) {
        Page<ActivityResponseDto> response = activityService.getCommunityActivities(communityId, pageable, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Registra o actualiza la confirmación de asistencia (RSVP) del usuario a una actividad.
     * Caso de Uso: CU-53.
     *
     * @param activityId UUID de la actividad.
     * @param currentUserId UUID del usuario autenticado.
     * @return Confirmación de participación y estado HTTP 200 (OK).
     */
    @PostMapping("/activities/{activityId}/participants")
    public ResponseEntity<ActivityParticipantResponseDto> confirmAttendance(
            @PathVariable UUID activityId,
            @RequestHeader("X-User-Id") UUID currentUserId) {
        ActivityParticipantResponseDto response = activityService.confirmAttendance(activityId, currentUserId);
        return ResponseEntity.ok(response);
    }
}
