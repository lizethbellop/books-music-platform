package com.musa.communities_service.controller;

import com.musa.communities_service.dto.request.CommunityCreateRequestDto;
import com.musa.communities_service.dto.request.CommunityUpdateRequestDto;
import com.musa.communities_service.dto.response.CommunityMemberResponseDto;
import com.musa.communities_service.dto.response.CommunityResponseDto;
import com.musa.communities_service.enums.CommunityType;
import com.musa.communities_service.service.CommunityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para la administración y consulta de comunidades y sus miembros.
 * Módulo de entrada para los casos de uso: CU-41, CU-42, CU-43, CU-44, CU-45 y CU-54.
 */
@RestController
@RequestMapping("/api/v1/communities")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    /**
     * Registra una nueva comunidad en el sistema.
     * Caso de Uso: CU-41.
     *
     * @param request Datos de creación validados.
     * @param currentUserId UUID del usuario autenticado recibido desde la API Gateway.
     * @return DTO con la comunidad creada y estado HTTP 201 (Created).
     */
    @PostMapping
    public ResponseEntity<CommunityResponseDto> createCommunity(
            @Valid @RequestBody CommunityCreateRequestDto request,
            @RequestHeader("X-User-Id") UUID currentUserId) {
        CommunityResponseDto response = communityService.createCommunity(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Busca y lista comunidades paginadas con filtros opcionales.
     * Caso de Uso: CU-42.
     *
     * @param query Texto opcional para buscar coincidencia en nombre o descripción.
     * @param type Tipo de comunidad opcional para filtrar (LIBROS, MUSICA, MIXTO).
     * @param pageable Configuración de paginación (por defecto tamaño 10 ordenado por createdAt DESC).
     * @param currentUserId UUID del usuario autenticado para calcular relaciones.
     * @return Página de comunidades mapeadas con estado HTTP 200 (OK).
     */
    @GetMapping
    public ResponseEntity<Page<CommunityResponseDto>> searchCommunities(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) CommunityType type,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @RequestHeader("X-User-Id") UUID currentUserId) {
        Page<CommunityResponseDto> response = communityService.searchCommunities(query, type, pageable, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene la información detallada de una comunidad por su ID.
     * Caso de Uso: CU-43.
     *
     * @param communityId UUID de la comunidad.
     * @param currentUserId UUID del usuario autenticado.
     * @return Detalle de la comunidad y estado HTTP 200 (OK).
     */
    @GetMapping("/{communityId}")
    public ResponseEntity<CommunityResponseDto> getCommunityById(
            @PathVariable UUID communityId,
            @RequestHeader("X-User-Id") UUID currentUserId) {
        CommunityResponseDto response = communityService.getCommunityById(communityId, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Actualiza la información de una comunidad existente.
     *
     * @param communityId UUID de la comunidad a actualizar.
     * @param request DTO con los datos modificables.
     * @param currentUserId UUID del usuario solicitante (debe ser ADMINISTRADOR).
     * @return Comunidad actualizada y estado HTTP 200 (OK).
     */
    @PutMapping("/{communityId}")
    public ResponseEntity<CommunityResponseDto> updateCommunity(
            @PathVariable UUID communityId,
            @Valid @RequestBody CommunityUpdateRequestDto request,
            @RequestHeader("X-User-Id") UUID currentUserId) {
        CommunityResponseDto response = communityService.updateCommunity(communityId, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Registra la adhesión del usuario autenticado a una comunidad.
     * Caso de Uso: CU-44.
     *
     * @param communityId UUID de la comunidad.
     * @param currentUserId UUID del usuario autenticado.
     * @return Membresía creada y estado HTTP 201 (Created).
     */
    @PostMapping("/{communityId}/members")
    public ResponseEntity<CommunityMemberResponseDto> joinCommunity(
            @PathVariable UUID communityId,
            @RequestHeader("X-User-Id") UUID currentUserId) {
        CommunityMemberResponseDto response = communityService.joinCommunity(communityId, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Procesa la salida voluntaria de un usuario de una comunidad.
     * Caso de Uso: CU-45.
     *
     * @param communityId UUID de la comunidad.
     * @param currentUserId UUID del usuario autenticado.
     * @return Estado HTTP 204 (No Content).
     */
    @DeleteMapping("/{communityId}/members/me")
    public ResponseEntity<Void> leaveCommunity(
            @PathVariable UUID communityId,
            @RequestHeader("X-User-Id") UUID currentUserId) {
        communityService.leaveCommunity(communityId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Realiza el borrado lógico de una comunidad en el sistema.
     * Caso de Uso: CU-54.
     *
     * @param communityId UUID de la comunidad a eliminar.
     * @param currentUserId UUID del usuario solicitante (debe ser ADMINISTRADOR).
     * @return Estado HTTP 204 (No Content).
     */
    @DeleteMapping("/{communityId}")
    public ResponseEntity<Void> deleteCommunity(
            @PathVariable UUID communityId,
            @RequestHeader("X-User-Id") UUID currentUserId) {
        communityService.deleteCommunity(communityId, currentUserId);
        return ResponseEntity.noContent().build();
    }
}