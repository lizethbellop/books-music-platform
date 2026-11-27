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
            "(cast(:query as String) IS NULL OR :query = '' " +
            "OR LOWER(c.name) LIKE LOWER(CONCAT('%', cast(:query as String), '%')) " +
            "OR LOWER(c.description) LIKE LOWER(CONCAT('%', cast(:query as String), '%')))")
    Page<Community> searchCommunities(
            @Param("query") String query,
            @Param("type") CommunityType type,
            @Param("status") CommunityStatus status,
            Pageable pageable);
}