package com.musa.communities_service.repository;

import com.musa.communities_service.entity.CommunityMember;
import com.musa.communities_service.enums.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la gestión de membresías de usuarios en comunidades
 * mediante la entidad {@link CommunityMember}.
 *
 * @author Plataforma Musa
 * @version 1.0
 */
@Repository
public interface CommunityMemberRepository extends JpaRepository<CommunityMember, UUID> {

    /**
     * Busca la membresía de un usuario específico dentro de una comunidad dada.
     *
     * @param communityId UUID de la comunidad.
     * @param userId UUID del usuario.
     * @return Un {@link Optional} con la membresía si existe.
     */
    Optional<CommunityMember> findByCommunityIdAndUserId(UUID communityId, UUID userId);

    /**
     * Verifica si un usuario es miembro activo de una comunidad específica.
     *
     * @param communityId UUID de la comunidad.
     * @param userId UUID del usuario.
     * @param status Estado de la membresía (ej. ACTIVO).
     * @return {@code true} si existe la membresía con el estado indicado.
     */
    boolean existsByCommunityIdAndUserIdAndStatus(UUID communityId, UUID userId, MemberStatus status);

    /**
     * Obtiene la lista paginada de miembros de una comunidad según su estado.
     *
     * @param communityId UUID de la comunidad a consultar.
     * @param status Estado de la membresía a filtrar.
     * @param pageable Configuración de paginación.
     * @return Página con los miembros encontrados.
     */
    Page<CommunityMember> findByCommunityIdAndStatus(UUID communityId, MemberStatus status, Pageable pageable);

    /**
     * Obtiene las membresías de un usuario en todas las comunidades donde participa según su estado.
     *
     * @param userId UUID del usuario.
     * @param status Estado de la membresía.
     * @param pageable Configuración de paginación.
     * @return Página con los registros de membresía del usuario.
     */
    Page<CommunityMember> findByUserIdAndStatus(UUID userId, MemberStatus status, Pageable pageable);

    /**
     * Cuenta el total de miembros vinculados a una comunidad con un estado determinado.
     *
     * @param communityId UUID de la comunidad.
     * @param status Estado de membresía a contabilizar.
     * @return Número total de miembros.
     */
    long countByCommunityIdAndStatus(UUID communityId, MemberStatus status);
}
