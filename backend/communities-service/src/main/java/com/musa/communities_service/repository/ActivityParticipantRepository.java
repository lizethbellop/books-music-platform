package com.musa.communities_service.repository;

import com.musa.communities_service.entity.ActivityParticipant;
import com.musa.communities_service.enums.ParticipantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la gestión de inscripciones y participantes
 * a las actividades de las comunidades {@link ActivityParticipant}.
 *
 * @author Plataforma Musa
 * @version 1.0
 */
@Repository
public interface ActivityParticipantRepository extends JpaRepository<ActivityParticipant, UUID> {

    /**
     * Obtiene el registro de participación de un usuario en una actividad determinada.
     *
     * @param activityId UUID de la actividad.
     * @param userId UUID del usuario.
     * @return Un {@link Optional} con el registro de participación.
     */
    Optional<ActivityParticipant> findByActivityIdAndUserId(UUID activityId, UUID userId);

    /**
     * Verifica si un usuario ya se encuentra registrado con un estado determinado en una actividad.
     *
     * @param activityId UUID de la actividad.
     * @param userId UUID del usuario.
     * @param status Estado de la participación (ej. CONFIRMADA).
     * @return {@code true} si la participación ya existe.
     */
    boolean existsByActivityIdAndUserIdAndStatus(UUID activityId, UUID userId, ParticipantStatus status);

    /**
     * Obtiene los participantes inscritos en una actividad específica según su estado.
     *
     * @param activityId UUID de la actividad.
     * @param status Estado de la participación.
     * @param pageable Configuración de paginación.
     * @return Página con los participantes de la actividad.
     */
    Page<ActivityParticipant> findByActivityIdAndStatus(UUID activityId, ParticipantStatus status, Pageable pageable);

    /**
     * Obtiene el historial de actividades donde ha participado un usuario.
     *
     * @param userId UUID del usuario.
     * @param status Estado de la participación.
     * @pageable Configuración de paginación.
     * @return Página con las participaciones del usuario.
     */
    Page<ActivityParticipant> findByUserIdAndStatus(UUID userId, ParticipantStatus status, Pageable pageable);

    /**
     * Cuenta el total de inscritos confirmados en una actividad.
     *
     * @param activityId UUID de la actividad.
     * @param status Estado de la participación.
     * @return Número total de asistentes confirmados.
     */
    long countByActivityIdAndStatus(UUID activityId, ParticipantStatus status);
}