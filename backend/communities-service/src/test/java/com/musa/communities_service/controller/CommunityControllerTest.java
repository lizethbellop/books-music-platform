package com.musa.communities_service.controller;

import com.musa.communities_service.dto.request.CommunityCreateRequestDto;
import com.musa.communities_service.dto.request.CommunityUpdateRequestDto;
import com.musa.communities_service.dto.response.CommunityMemberResponseDto;
import com.musa.communities_service.dto.response.CommunityResponseDto;
import com.musa.communities_service.enums.CommunityStatus;
import com.musa.communities_service.enums.CommunityType;
import com.musa.communities_service.enums.CommunityVisibility;
import com.musa.communities_service.enums.MemberRole;
import com.musa.communities_service.enums.MemberStatus;
import com.musa.communities_service.service.CommunityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias de la capa Web para CommunityController usando MockMvc.
 * Valida los endpoints REST y la inyección de cabeceras de contexto (X-User-Id).
 */
@WebMvcTest(CommunityController.class)
class CommunityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommunityService communityService;

    private UUID communityId;
    private UUID userId;
    private CommunityResponseDto communityResponseDto;

    @BeforeEach
    void setUp() {
        communityId = UUID.randomUUID();
        userId = UUID.randomUUID();

        communityResponseDto = new CommunityResponseDto(
                communityId,
                "Club de Lectura Musa",
                "Comunidad para apasionados de la literatura.",
                CommunityType.LIBROS,
                CommunityVisibility.PUBLICA,
                userId,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                CommunityStatus.ACTIVA,
                10L,
                true,
                MemberRole.ADMINISTRADOR
        );
    }

    @Test
    @DisplayName("CU-41: Registrar comunidad debe retornar HTTP 201 Created cuando los datos son válidos")
    void createCommunity_debeRetornar201Created_cuandoPeticionEsValida() throws Exception {
        // ARRANGE (Preparación): configurar la simulación del servicio para que al recibir cualquier DTO y el ID de usuario devuelva la representación completa de la comunidad creada.
        when(communityService.createCommunity(any(CommunityCreateRequestDto.class), eq(userId)))
                .thenReturn(communityResponseDto);

        String jsonBody = """
                {
                  "name": "Club de Lectura Musa",
                  "description": "Comunidad para apasionados de la literatura.",
                  "type": "LIBROS",
                  "visibility": "PUBLICA"
                }
                """;

        // ACT (Ejecución): realizar la petición POST simulando la llamada del API Gateway con el header X-User-Id.
        var response = mockMvc.perform(post("/api/v1/communities")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody));

        // ASSERT (Verificación): comprobar que responda con código HTTP 201 (Created), formato JSON y que los atributos coincidan con los datos devueltos por el servicio.
        response.andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(communityId.toString()))
                .andExpect(jsonPath("$.name").value("Club de Lectura Musa"))
                .andExpect(jsonPath("$.type").value("LIBROS"))
                .andExpect(jsonPath("$.userRole").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("CU-42: Consultar comunidades paginadas debe retornar HTTP 200 OK con la lista de resultados")
    void searchCommunities_debeRetornarPaginaDeComunidades_conEstado200OK() throws Exception {
        // ARRANGE (Preparación): envolver el DTO en un objeto PageImpl de Spring Data para simular el retorno paginado del servicio.
        Page<CommunityResponseDto> pageResponse = new PageImpl<>(List.of(communityResponseDto));
        when(communityService.searchCommunities(eq("Lectura"), eq(CommunityType.LIBROS), any(Pageable.class), eq(userId)))
                .thenReturn(pageResponse);

        // ACT (Ejecución): enviar petición GET con parámetros de búsqueda de query, tipo y paginación.
        var response = mockMvc.perform(get("/api/v1/communities")
                .param("query", "Lectura")
                .param("type", "LIBROS")
                .param("page", "0")
                .param("size", "10")
                .header("X-User-Id", userId.toString()));

        // ASSERT (Verificación): validar respuesta HTTP 200 OK y la estructura paginada del JSON (content, totalElements).
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(communityId.toString()))
                .andExpect(jsonPath("$.content[0].name").value("Club de Lectura Musa"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("CU-43: Obtener detalle de comunidad por ID debe retornar HTTP 200 OK")
    void getCommunityById_debeRetornar200OK_cuandoExisteComunidad() throws Exception {
        // ARRANGE (Preparación): instruct a Mockito a devolver el DTO de la comunidad cuando se consulte su UUID.

        when(communityService.getCommunityById(eq(communityId), eq(userId)))
                .thenReturn(communityResponseDto);

        // ACT (Ejecución): ejecutar la consulta HTTP GET a la ruta específica /{communityId}.
        var response = mockMvc.perform(get("/api/v1/communities/{communityId}", communityId)
                .header("X-User-Id", userId.toString()));

        // ASSERT (Verificación): verificar código 200 OK y la correspondencia de la comunidad obtenida.
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(communityId.toString()))
                .andExpect(jsonPath("$.name").value("Club de Lectura Musa"));
    }

    @Test
    @DisplayName("Actualizar comunidad debe retornar HTTP 200 OK con los datos modificados")
    void updateCommunity_debeRetornar200OK_cuandoEdicionEsExitosa() throws Exception {
        // ARRANGE (Preparación): definir la respuesta modificada esperada del servicio al procesar la actualización.
        CommunityResponseDto updatedResponse = new CommunityResponseDto(
                communityId,
                "Club de Lectura Avanzada",
                "Descripción actualizada",
                CommunityType.LIBROS,
                CommunityVisibility.PUBLICA,
                userId,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                CommunityStatus.ACTIVA,
                10L,
                true,
                MemberRole.ADMINISTRADOR
        );

        when(communityService.updateCommunity(eq(communityId), any(CommunityUpdateRequestDto.class), eq(userId)))
                .thenReturn(updatedResponse);

        String jsonBody = """
                {
                  "name": "Club de Lectura Avanzada",
                  "description": "Descripción actualizada",
                  "visibility": "PUBLICA"
                }
                """;

        // ACT (Ejecución): ejecutar petición HTTP PUT pasando el cuerpo JSON y los headers de autenticación.
        var response = mockMvc.perform(put("/api/v1/communities/{communityId}", communityId)
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody));

        // ASSERT (Verificación): confirmar estado 200 OK y reflejar el nombre actualizado en la respuesta.
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Club de Lectura Avanzada"));
    }

    @Test
    @DisplayName("CU-44: Unirse a comunidad debe retornar HTTP 201 Created con el detalle de membresía")
    void joinCommunity_debeRetornar201Created_cuandoSeUneExitosamente() throws Exception {
        // ARRANGE (Preparación): construir el DTO simulado de la relación de membresía generada para el usuario.
        UUID memberId = UUID.randomUUID();
        CommunityMemberResponseDto memberResponse = new CommunityMemberResponseDto(
                memberId,
                communityId,
                userId,
                MemberRole.MIEMBRO,
                OffsetDateTime.now(),
                MemberStatus.ACTIVO
        );

        when(communityService.joinCommunity(eq(communityId), eq(userId)))
                .thenReturn(memberResponse);

        // ACT (Ejecución): invocar el endpoint de adhesión POST /{communityId}/members.
        var response = mockMvc.perform(post("/api/v1/communities/{communityId}/members", communityId)
                .header("X-User-Id", userId.toString()));

        // ASSERT (Verificación): comprobar estado HTTP 201 Created y los valores de la membresía (rol MIEMBRO).
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(memberId.toString()))
                .andExpect(jsonPath("$.role").value("MIEMBRO"))
                .andExpect(jsonPath("$.status").value("ACTIVO"));
    }

    @Test
    @DisplayName("CU-45: Salir de la comunidad debe retornar HTTP 204 No Content")
    void leaveCommunity_debeRetornar204NoContent_cuandoSalidaEsExitosa() throws Exception {
        // ARRANGE (Preparación): Indicar a Mockito que el método de salida del servicio no devuelve contenido (doNothing).
        doNothing().when(communityService).leaveCommunity(communityId, userId);

        // ACT (Ejecución): enviar solicitud DELETE /{communityId}/members/me.
        var response = mockMvc.perform(delete("/api/v1/communities/{communityId}/members/me", communityId)
                .header("X-User-Id", userId.toString()));

        // ASSERT (Verificación): validar que la respuesta retorne un código vacío 204 No Content.
        response.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("CU-54: Borrado lógico de comunidad debe retornar HTTP 204 No Content")
    void deleteCommunity_debeRetornar204NoContent_cuandoEliminacionEsExitosa() throws Exception {
        // ARRANGE (Preparación): Configurar el servicio para ejecutar la baja lógica sin lanzar excepciones.
        doNothing().when(communityService).deleteCommunity(communityId, userId);

        // ACT (Ejecución): ejecutar solicitud HTTP DELETE al recurso de la comunidad.
        var response = mockMvc.perform(delete("/api/v1/communities/{communityId}", communityId)
                .header("X-User-Id", userId.toString()));

        // ASSERT (Verificación): validar respuesta vacía 204 No Content confirmando la baja del recurso.
        response.andExpect(status().isNoContent());
    }
}