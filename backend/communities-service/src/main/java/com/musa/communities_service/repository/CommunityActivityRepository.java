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
     * Obtiene una lista paginada de actividades de una comunidad que no tengan un estado específico.
     *
     * @param communityId UUID de la comunidad.
     * @param status Estado a excluir (ej. CANCELADA).
     * @param pageable Parámetros de paginación y ordenamiento.
     * @return Página con las actividades que no coinciden con el estado excluido.
     */
    Page<CommunityActivity> findByCommunityIdAndStatusNot(UUID communityId, ActivityStatus status, Pageable pageable);


    /**
     * Busca una actividad específica perteneciente a una comunidad dada por sus identificadores y que no tenga un estado específico.
     *
     * @param id UUID de la actividad.
     * @param communityId UUID de la comunidad organizadora.
     * @param status Estado lógico a excluir de la búsqueda.
     * @return Un {@link Optional} con la actividad localizada o vacío si no coincide.
     */
    Optional<CommunityActivity> findByIdAndCommunityIdAndStatusNot(UUID id, UUID communityId, ActivityStatus status);
}