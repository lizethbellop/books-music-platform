package com.musa.communities_service.repository;

import com.musa.communities_service.entity.Community;
import com.musa.communities_service.enums.CommunityStatus;
import com.musa.communities_service.enums.CommunityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la gestión del ciclo de vida y consultas
 * de persistencia de la entidad {@link Community}.
 *
 * @author Plataforma Musa
 * @version 1.0
 */
@Repository
public interface CommunityRepository extends JpaRepository<Community, UUID> {

    /**
     * Verifica si existe una comunidad registrada con un nombre específico,
     * ignorando diferencias entre mayúsculas y minúsculas.
     *
     * @param name Nombre de la comunidad a validar.
     * @return {@code true} si el nombre ya existe, {@code false} de lo contrario.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Busca una comunidad por su identificador único y por un estado lógico específico.
     *
     * @param id Identificador UUID de la comunidad.
     * @param status Estado de la comunidad (ej. ACTIVA).
     * @return Un {@link Optional} con la comunidad encontrada o vacío si no coincide.
     */
    Optional<Community> findByIdAndStatus(UUID id, CommunityStatus status);

    /**
     * Obtiene una lista paginada de comunidades filtradas por su estado lógico.
     *
     * @param status Estado lógico por el cual filtrar (ej. ACTIVA).
     * @param pageable Configuración de paginación y ordenamiento.
     * @return Página de comunidades encontradas.
     */
    Page<Community> findByStatus(CommunityStatus status, Pageable pageable);

    /**
     * Obtiene una lista paginada de comunidades filtradas por su tipo y estado lógico.
     *
     * @param type Tipo de comunidad (LIBROS, MUSICA, MIXTO).
     * @param status Estado lógico por el cual filtrar.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return Página de comunidades encontradas.
     */
    Page<Community> findByTypeAndStatus(CommunityType type, CommunityStatus status, Pageable pageable);

    /**
     * Obtiene las comunidades creadas por un usuario específico filtradas por su estado.
     *
     * @param createdBy UUID del usuario creador.
     * @param status Estado lógico de la comunidad.
     * @param pageable Configuración de paginación.
     * @return Página con las comunidades del usuario creador.
     */
    Page<Community> findByCreatedByAndStatus(UUID createdBy, CommunityStatus status, Pageable pageable);
}