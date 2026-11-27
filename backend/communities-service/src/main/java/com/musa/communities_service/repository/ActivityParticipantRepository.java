package com.musa.communities_service.repository;

import com.musa.communities_service.entity.ActivityParticipant;
import com.musa.communities_service.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Cuenta el total de inscritos confirmados en una actividad.
     *
     * @param activityId UUID de la actividad.
     * @param status Estado de la participación.
     * @return Número total de asistentes confirmados.
     */
    long countByActivityIdAndStatus(UUID activityId, ParticipantStatus status);

    /**
     * Obtiene el registro de participación de un usuario en una actividad específica filtrando por estado.
     *
     * @param activityId UUID de la actividad.
     * @param userId UUID del usuario.
     * @param status Estado de la participación (ej. CONFIRMADA).
     * @return Un {@link Optional} con el registro de participación si coincide con el estado.
     */
    Optional<ActivityParticipant> findByActivityIdAndUserIdAndStatus(UUID activityId, UUID userId, ParticipantStatus status);

    /**
     * Actualiza el estado de participación de todos los registros asociados a una actividad específica.
     *
     * @param activityId UUID de la actividad.
     * @param currentStatus Estado actual de los registros a actualizar.
     * @param newStatus Nuevo estado que se asignará a los registros.
     * @return Número de registros afectados por la actualización.
     */
    @Modifying
    @Query("UPDATE ActivityParticipant p SET p.status = :newStatus WHERE p.activity.id = :activityId AND p.status = :currentStatus")
    int updateStatusByActivityIdAndStatus(
            @Param("activityId") UUID activityId,
            @Param("currentStatus") ParticipantStatus currentStatus,
            @Param("newStatus") ParticipantStatus newStatus);
}