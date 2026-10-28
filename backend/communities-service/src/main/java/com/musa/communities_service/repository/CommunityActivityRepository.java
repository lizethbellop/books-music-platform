package com.musa.communities_service.repository;

import com.musa.communities_service.entity.CommunityActivity;
import com.musa.communities_service.enums.ActivityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la administración de eventos y actividades
 * asociadas a comunidades {@link CommunityActivity}.
 *
 * @author Plataforma Musa
 * @version 1.0
 */
@Repository
public interface CommunityActivityRepository extends JpaRepository<CommunityActivity, UUID> {

    /**
     * Busca una actividad por su ID y su estado.
     *
     * @param id UUID de la actividad.
     * @param status Estado de la actividad.
     * @return Un {@link Optional} con la actividad si fue localizada.
     */
    Optional<CommunityActivity> findByIdAndStatus(UUID id, ActivityStatus status);

    /**
     * Obtiene las actividades programadas u organizadas en una comunidad específica.
     *
     * @param communityId UUID de la comunidad asociada.
     * @param status Estado de la actividad (PROGRAMADA, CANCELADA, COMPLETADA).
     * @param pageable Configuración de paginación y ordenamiento cronológico.
     * @return Página de actividades pertenecientes a la comunidad.
     */
    Page<CommunityActivity> findByCommunityIdAndStatus(UUID communityId, ActivityStatus status, Pageable pageable);

    /**
     * Lista las actividades creadas por un usuario en particular dentro de una comunidad.
     *
     * @param communityId UUID de la comunidad.
     * @param createdBy UUID del usuario organizador.
     * @param status Estado de la actividad.
     * @param pageable Configuración de paginación.
     * @return Página con las actividades creadas por el usuario.
     */
    Page<CommunityActivity> findByCommunityIdAndCreatedByAndStatus(UUID communityId, UUID createdBy, ActivityStatus status, Pageable pageable);
}