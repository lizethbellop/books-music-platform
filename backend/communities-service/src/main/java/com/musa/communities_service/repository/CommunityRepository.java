package com.musa.communities_service.repository;

import com.musa.communities_service.entity.Community;
import com.musa.communities_service.enums.CommunityStatus;
import com.musa.communities_service.enums.CommunityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la persistencia, consulta y gestión del ciclo
 * de vida de la entidad {@link Community}.
 */
@Repository
public interface CommunityRepository extends JpaRepository<Community, UUID> {

    /**
     * Verifica si existe una comunidad registrada con el nombre especificado, ignorando diferencias
     * entre mayúsculas y minúsculas y filtrando por su estado lógico.
     *
     * @param name Nombre de la comunidad a validar.
     * @param status Estado lógico de la comunidad (ej. ACTIVA).
     * @return {@code true} si existe una coincidencia, {@code false} de lo contrario.
     */
    boolean existsByNameIgnoreCaseAndStatus(String name, CommunityStatus status);

    /**
     * Busca una comunidad por su identificador único y su estado lógico.
     *
     * @param id Identificador UUID de la comunidad.
     * @param status Estado lógico requerido.
     * @return Un {@link Optional} con la comunidad encontrada o vacío si no existe.
     */
    Optional<Community> findByIdAndStatus(UUID id, CommunityStatus status);

    /**
     * Obtiene una lista paginada de comunidades filtradas exclusivamente por su estado lógico.
     *
     * @param status Estado lógico por el cual filtrar.
     * @param pageable Parámetros de paginación y ordenamiento.
     * @return Página de comunidades que coinciden con el estado.
     */
    Page<Community> findByStatus(CommunityStatus status, Pageable pageable);

    /**
     * Obtiene una lista paginada de comunidades filtradas por su tipo y estado lógico.
     *
     * @param type Tipo de comunidad (LIBROS, MUSICA, MIXTO).
     * @param status Estado lógico por el cual filtrar.
     * @param pageable Parámetros de paginación y ordenamiento.
     * @return Página de comunidades encontradas.
     */
    Page<Community> findByTypeAndStatus(CommunityType type, CommunityStatus status, Pageable pageable);

    /**
     * Consulta las comunidades creadas por un usuario específico filtradas por su estado lógico.
     *
     * @param createdBy UUID del usuario creador.
     * @param status Estado lógico de la comunidad.
     * @param pageable Parámetros de paginación.
     * @return Página con las comunidades creadas por el usuario.
     */
    Page<Community> findByCreatedByAndStatus(UUID createdBy, CommunityStatus status, Pageable pageable);

    /**
     * Realiza una búsqueda avanzada paginada de comunidades aplicando filtros opcionales de texto
     * (coincidencia en nombre o descripción) y tipo de comunidad sobre registros en un estado determinado.
     *
     * @param query Texto de búsqueda opcional para coincidencia parcial en nombre o descripción.
     * @param type Tipo de comunidad opcional para filtrar.
     * @param status Estado lógico requerido para la consulta.
     * @param pageable Parámetros de paginación y ordenamiento.
     * @return Página de comunidades filtradas según los criterios establecidos.
     */
    @Query("SELECT c FROM Community c WHERE c.status = :status AND " +
            "(:type IS NULL OR c.type = :type) AND " +
            "(:query IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Community> searchCommunities(
            @Param("query") String query,
            @Param("type") CommunityType type,
            @Param("status") CommunityStatus status,
            Pageable pageable);
}