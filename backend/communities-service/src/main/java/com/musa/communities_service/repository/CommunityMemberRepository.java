package com.musa.communities_service.repository;

import com.musa.communities_service.entity.CommunityMember;
import com.musa.communities_service.enums.MemberRole;
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
     * Consulta el registro de membresía de un usuario en una comunidad sin importar su estado actual.
     *
     * @param communityId UUID de la comunidad.
     * @param userId UUID del usuario.
     * @return Un {@link Optional} con el registro de membresía si existe.
     */
    Optional<CommunityMember> findByCommunityIdAndUserId(UUID communityId, UUID userId);

    /**
     * Consulta la membresía de un usuario dentro de una comunidad filtrando por un estado específico.
     *
     * @param communityId UUID de la comunidad.
     * @param userId UUID del usuario.
     * @param status Estado de la membresía (ej. ACTIVO).
     * @return Un {@link Optional} con la membresía encontrada o vacío si no coincide.
     */
    Optional<CommunityMember> findByCommunityIdAndUserIdAndStatus(UUID communityId, UUID userId, MemberStatus status);

    /**
     * Verifica si un usuario posee una membresía en un estado determinado dentro de una comunidad.
     *
     * @param communityId UUID de la comunidad.
     * @param userId UUID del usuario.
     * @param status Estado de la membresía a verificar.
     * @return {@code true} si existe el registro con dicho estado, {@code false} de lo contrario.
     */
    boolean existsByCommunityIdAndUserIdAndStatus(UUID communityId, UUID userId, MemberStatus status);

    /**
     * Verifica si un usuario posee un rol y un estado de membresía específicos dentro de una comunidad.
     * Utilizado principalmente para la validación de permisos de administración.
     *
     * @param communityId UUID de la comunidad.
     * @param userId UUID del usuario.
     * @param role Rol requerido (ej. ADMINISTRADOR).
     * @param status Estado de la membresía requerido (ej. ACTIVO).
     * @return {@code true} si el usuario cumple con el rol y estado indicados.
     */
    boolean existsByCommunityIdAndUserIdAndRoleAndStatus(UUID communityId, UUID userId, MemberRole role, MemberStatus status);

    /**
     * Obtiene una lista paginada de miembros asociados a una comunidad según su estado de membresía.
     *
     * @param communityId UUID de la comunidad.
     * @param status Estado de membresía por el cual filtrar.
     * @param pageable Parámetros de paginación.
     * @return Página con los miembros encontrados.
     */
    Page<CommunityMember> findByCommunityIdAndStatus(UUID communityId, MemberStatus status, Pageable pageable);

    /**
     * Obtiene las membresías registradas de un usuario en todas las comunidades filtradas por su estado.
     *
     * @param userId UUID del usuario.
     * @param status Estado de la membresía a consultar.
     * @param pageable Parámetros de paginación.
     * @return Página con las membresías asociadas al usuario.
     */
    Page<CommunityMember> findByUserIdAndStatus(UUID userId, MemberStatus status, Pageable pageable);

    /**
     * Cuenta la cantidad total de miembros vinculados a una comunidad con un estado determinado.
     *
     * @param communityId UUID de la comunidad.
     * @param status Estado de la membresía a contabilizar.
     * @return Total de miembros encontrados.
     */
    long countByCommunityIdAndStatus(UUID communityId, MemberStatus status);

    /**
     * Cuenta el total de miembros en una comunidad que poseen un rol y estado específicos.
     *
     * @param communityId UUID de la comunidad.
     * @param role Rol de membresía a contabilizar.
     * @param status Estado de la membresía a contabilizar.
     * @return Total de miembros que cumplen con los criterios.
     */
    long countByCommunityIdAndRoleAndStatus(UUID communityId, MemberRole role, MemberStatus status);
}