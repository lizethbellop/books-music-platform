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
 * Repositorio Spring Data JPA para la administración y consulta de eventos y actividades
 * asociadas a las comunidades mediante la entidad {@link CommunityActivity}.
 */
@Repository
public interface CommunityActivityRepository extends JpaRepository<CommunityActivity, UUID> {

    /**
     * Busca una actividad por su identificador único y estado lógico.
     *
     * @param id UUID de la actividad.
     * @param status Estado de la actividad (ej. PROGRAMADA).
     * @return Un {@link Optional} con la actividad si fue localizada.
     */
    Optional<CommunityActivity> findByIdAndStatus(UUID id, ActivityStatus status);

    /**
     * Busca una actividad específica perteneciente a una comunidad dada por sus identificadores y estado.
     *
     * @param id UUID de la actividad.
     * @param communityId UUID de la comunidad organizadora.
     * @param status Estado lógico de la actividad.
     * @return Un {@link Optional} con la actividad localizada o vacío si no coincide.
     */
    Optional<CommunityActivity> findByIdAndCommunityIdAndStatus(UUID id, UUID communityId, ActivityStatus status);

    /**
     * Obtiene una lista paginada de actividades pertenecientes a una comunidad según su estado.
     *
     * @param communityId UUID de la comunidad asociada.
     * @param status Estado lógico de la actividad.
     * @param pageable Parámetros de paginación y ordenamiento.
     * @return Página de actividades asociadas a la comunidad.
     */
    Page<CommunityActivity> findByCommunityIdAndStatus(UUID communityId, ActivityStatus status, Pageable pageable);

    /**
     * Obtiene las actividades creadas por un usuario en particular dentro de una comunidad específica y con un estado dado.
     *
     * @param communityId UUID de la comunidad.
     * @param createdBy UUID del usuario organizador.
     * @param status Estado de la actividad.
     * @param pageable Parámetros de paginación.
     * @return Página con las actividades que coinciden con los parámetros.
     */
    Page<CommunityActivity> findByCommunityIdAndCreatedByAndStatus(UUID communityId, UUID createdBy, ActivityStatus status, Pageable pageable);
}