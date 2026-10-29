package com.musa.communities_service.service;

import com.musa.communities_service.dto.request.ActivityCreateRequestDto;
import com.musa.communities_service.dto.request.ActivityUpdateRequestDto;
import com.musa.communities_service.dto.response.ActivityParticipantResponseDto;
import com.musa.communities_service.dto.response.ActivityResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Contrato de servicio para la gestión de la agenda de actividades y asistencia de usuarios.
 */
public interface CommunityActivityService {

    /**
     * Programa una nueva actividad o evento dentro del marco de una comunidad.
     *
     * @param communityId Identificador de la comunidad organizadora.
     * @param request Datos del formulario de creación de la actividad.
     * @param currentUserId Identificador del usuario creador (debe ser ADMINISTRADOR de la comunidad).
     * @return DTO con la información de la actividad programada.
     */
    ActivityResponseDto createActivity(UUID communityId, ActivityCreateRequestDto request, UUID currentUserId);

    /**
     * Modifica los datos de una actividad previamente programada.
     *
     * @param communityId Identificador de la comunidad organizadora.
     * @param activityId Identificador de la actividad a modificar.
     * @param request Datos del formulario de edición de la actividad.
     * @param currentUserId Identificador del usuario que ejecuta la modificación (debe ser ADMINISTRADOR).
     * @return DTO con la información de la actividad actualizada.
     */
    ActivityResponseDto updateActivity(UUID communityId, UUID activityId, ActivityUpdateRequestDto request, UUID currentUserId);

    /**
     * Cancela una actividad programada actualizando su estado a CANCELADA.
     *
     * @param communityId Identificador de la comunidad organizadora.
     * @param activityId Identificador de la actividad a cancelar.
     * @param currentUserId Identificador del usuario que solicita la cancelación (debe ser ADMINISTRADOR).
     */
    void deleteActivity(UUID communityId, UUID activityId, UUID currentUserId);

    /**
     * Consulta la lista paginada de actividades programadas para una comunidad específica.
     *
     * @param communityId Identificador de la comunidad organizadora.
     * @param pageable Parámetros de paginación y ordenamiento.
     * @param currentUserId Identificador del usuario autenticado para verificar si asiste a cada actividad.
     * @return Página con el listado de actividades de la comunidad.
     */
    Page<ActivityResponseDto> getCommunityActivities(UUID communityId, Pageable pageable, UUID currentUserId);

    /**
     * Registra o actualiza la confirmación de asistencia (RSVP) del usuario autenticado a una actividad.
     *
     * @param activityId Identificador de la actividad a la cual asistirá.
     * @param currentUserId Identificador del usuario autenticado que confirma asistencia.
     * @return DTO con la confirmación de participación registrada.
     */
    ActivityParticipantResponseDto confirmAttendance(UUID activityId, UUID currentUserId);
}
