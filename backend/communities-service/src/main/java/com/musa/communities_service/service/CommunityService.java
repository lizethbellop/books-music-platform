package com.musa.communities_service.service;

import com.musa.communities_service.dto.request.CommunityCreateRequestDto;
import com.musa.communities_service.dto.request.CommunityUpdateRequestDto;
import com.musa.communities_service.dto.response.CommunityMemberResponseDto;
import com.musa.communities_service.dto.response.CommunityResponseDto;
import com.musa.communities_service.enums.CommunityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Contrato de servicio para la gestión de comunidades y sus membresías.
 */
public interface CommunityService {

    /**
     * Registra una nueva comunidad en la plataforma y asigna automáticamente
     * al usuario creador con el rol de ADMINISTRADOR.
     *
     * @param request Datos del formulario de creación de la comunidad.
     * @param currentUserId Identificador del usuario autenticado que crea la comunidad.
     * @return DTO de respuesta con el detalle completo de la comunidad creada.
     */
    CommunityResponseDto createCommunity(CommunityCreateRequestDto request, UUID currentUserId);

    /**
     * Busca y lista comunidades de forma paginada aplicando filtros opcionales por texto o tipo de comunidad.
     *
     * @param query Texto opcional para buscar por coincidencia en nombre o descripción.
     * @param type Tipo de comunidad opcional para filtrar (LIBROS, MUSICA, MIXTO).
     * @param pageable Parámetros de paginación y ordenamiento.
     * @param currentUserId Identificador del usuario autenticado para calcular su estado de membresía.
     * @return Página de comunidades que coinciden con los criterios.
     */
    Page<CommunityResponseDto> searchCommunities(String query, CommunityType type, Pageable pageable, UUID currentUserId);

    /**
     * Obtiene la información detallada de una comunidad por su identificador único.
     *
     * @param communityId Identificador único de la comunidad.
     * @param currentUserId Identificador del usuario autenticado para calcular su rol y membresía.
     * @return DTO de respuesta con el detalle de la comunidad.
     */
    CommunityResponseDto getCommunityById(UUID communityId, UUID currentUserId);

    /**
     * Actualiza la información modificable de una comunidad existente.
     *
     * @param communityId Identificador de la comunidad a actualizar.
     * @param request Datos del formulario de actualización.
     * @param currentUserId Identificador del usuario que ejecuta la acción (debe ser ADMINISTRADOR).
     * @return DTO de respuesta con los datos actualizados de la comunidad.
     */
    CommunityResponseDto updateCommunity(UUID communityId, CommunityUpdateRequestDto request, UUID currentUserId);

    /**
     * Agrega al usuario autenticado como miembro activo de una comunidad.
     *
     * @param communityId Identificador de la comunidad a la que desea unirse.
     * @param currentUserId Identificador del usuario autenticado.
     * @return DTO con la información de la membresía registrada.
     */
    CommunityMemberResponseDto joinCommunity(UUID communityId, UUID currentUserId);

    /**
     * Procesa el retiro voluntario de un usuario de una comunidad.
     *
     * @param communityId Identificador de la comunidad de la que se retira.
     * @param currentUserId Identificador del usuario autenticado.
     */
    void leaveCommunity(UUID communityId, UUID currentUserId);

    /**
     * Realiza la eliminación lógica de una comunidad cambiando su estado a ELIMINADA.
     *
     * @param communityId Identificador de la comunidad a eliminar.
     * @param currentUserId Identificador del usuario autenticado (debe ser ADMINISTRADOR).
     */
    void deleteCommunity(UUID communityId, UUID currentUserId);
}