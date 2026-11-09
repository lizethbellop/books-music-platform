package com.musa.communities_service.controller;

import com.musa.communities_service.dto.request.ActivityCreateRequestDto;
import com.musa.communities_service.dto.request.ActivityUpdateRequestDto;
import com.musa.communities_service.dto.response.ActivityParticipantResponseDto;
import com.musa.communities_service.dto.response.ActivityResponseDto;
import com.musa.communities_service.enums.ActivityStatus;
import com.musa.communities_service.enums.ParticipantStatus;
import com.musa.communities_service.service.CommunityActivityService;
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

@WebMvcTest(CommunityActivityController.class)
class CommunityActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommunityActivityService activityService;

    private UUID communityId;
    private UUID activityId;
    private UUID userId;
    private ActivityResponseDto activityResponseDto;

    @BeforeEach
    void setUp() {
        communityId = UUID.randomUUID();
        activityId = UUID.randomUUID();
        userId = UUID.randomUUID();

        activityResponseDto = new ActivityResponseDto(
                activityId,
                communityId,
                userId,
                "Debate: Cien Años de Soledad",
                "Discusión sobre los capítulos 1 al 5",
                "Sala Virtual Zoom",
                OffsetDateTime.parse("2026-11-15T18:00:00Z"),
                OffsetDateTime.parse("2026-11-15T20:00:00Z"),
                OffsetDateTime.parse("2026-10-29T10:00:00Z"),
                OffsetDateTime.parse("2026-10-29T10:00:00Z"),
                ActivityStatus.PROGRAMADA,
                5L,
                true
        );
    }

    @Test
    @DisplayName("CU-49: Crear actividad debe retornar HTTP 201 Created cuando los datos son válidos")
    void createActivity_debeRetornar201Created_cuandoPeticionEsValida() throws Exception {
        // ARRANGE
        when(activityService.createActivity(eq(communityId), any(ActivityCreateRequestDto.class), eq(userId)))
                .thenReturn(activityResponseDto);

        String jsonBody = """
                {
                  "title": "Debate: Cien Años de Soledad",
                  "description": "Discusión sobre los capítulos 1 al 5",
                  "location": "Sala Virtual Zoom",
                  "startsAt": "2026-11-15T18:00:00Z",
                  "endsAt": "2026-11-15T20:00:00Z"
                }
                """;

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/communities/{communityId}/activities", communityId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(activityId.toString()))
                .andExpect(jsonPath("$.title").value("Debate: Cien Años de Soledad"))
                .andExpect(jsonPath("$.status").value("PROGRAMADA"))
                .andExpect(jsonPath("$.participantCount").value(5))
                .andExpect(jsonPath("$.isParticipating").value(true));
    }

    @Test
    @DisplayName("CU-50: Actualizar actividad debe retornar HTTP 200 OK")
    void updateActivity_debeRetornar200OK_cuandoActualizacionEsExitosa() throws Exception {
        // ARRANGE
        when(activityService.updateActivity(eq(communityId), eq(activityId), any(ActivityUpdateRequestDto.class), eq(userId)))
                .thenReturn(activityResponseDto);

        String jsonBody = """
                {
                  "title": "Debate Ampliado: Cien Años de Soledad",
                  "description": "Se analizarán los capítulos del 1 al 10.",
                  "location": "Google Meet",
                  "startsAt": "2026-11-15T18:30:00Z",
                  "endsAt": "2026-11-15T21:00:00Z"
                }
                """;

        // ACT & ASSERT
        mockMvc.perform(put("/api/v1/communities/{communityId}/activities/{activityId}", communityId, activityId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activityId.toString()))
                .andExpect(jsonPath("$.title").value("Debate: Cien Años de Soledad"));
    }

    @Test
    @DisplayName("CU-51: Eliminar actividad debe retornar HTTP 204 No Content")
    void deleteActivity_debeRetornar204NoContent_cuandoCancelacionEsExitosa() throws Exception {
        // ARRANGE
        doNothing().when(activityService).deleteActivity(communityId, activityId, userId);

        // ACT & ASSERT
        mockMvc.perform(delete("/api/v1/communities/{communityId}/activities/{activityId}", communityId, activityId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("CU-52: Obtener agenda de actividades paginada debe retornar HTTP 200 OK")
    void getCommunityActivities_debeRetornarPaginaDeActividades_conEstado200OK() throws Exception {
        // ARRANGE
        Page<ActivityResponseDto> pageResponse = new PageImpl<>(List.of(activityResponseDto));
        when(activityService.getCommunityActivities(eq(communityId), any(Pageable.class), eq(userId)))
                .thenReturn(pageResponse);

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/communities/{communityId}/activities", communityId)
                        .param("page", "0")
                        .param("size", "10")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(activityId.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Debate: Cien Años de Soledad"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("CU-53: Confirmar asistencia RSVP debe retornar HTTP 200 OK")
    void confirmAttendance_debeRetornar200OK_conEstadoConfirmada() throws Exception {
        // ARRANGE
        UUID participantId = UUID.randomUUID();
        ActivityParticipantResponseDto participantResponse = new ActivityParticipantResponseDto(
                participantId,
                activityId,
                userId,
                ParticipantStatus.CONFIRMADA,
                OffsetDateTime.parse("2026-10-29T10:00:00Z")
        );

        when(activityService.confirmAttendance(eq(activityId), eq(userId)))
                .thenReturn(participantResponse);

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/activities/{activityId}/participants", activityId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(participantId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMADA"));
    }
}
