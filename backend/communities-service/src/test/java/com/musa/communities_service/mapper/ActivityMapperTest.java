package com.musa.communities_service.mapper;

import com.musa.communities_service.dto.request.ActivityCreateRequestDto;
import com.musa.communities_service.dto.response.ActivityParticipantResponseDto;
import com.musa.communities_service.dto.response.ActivityResponseDto;
import com.musa.communities_service.entity.ActivityParticipant;
import com.musa.communities_service.entity.Community;
import com.musa.communities_service.entity.CommunityActivity;
import com.musa.communities_service.enums.ActivityStatus;
import com.musa.communities_service.enums.CommunityType;
import com.musa.communities_service.enums.CommunityVisibility;
import com.musa.communities_service.enums.ParticipantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar las transformaciones de objetos en ActivityMapper.
 */
class ActivityMapperTest {

    private ActivityMapper activityMapper;

    @BeforeEach
    void setUp() {
        activityMapper = new ActivityMapper();
    }

    @Test
    @DisplayName("toEntity: Debe mapear la solicitud de creación a una Entidad CommunityActivity")
    void toEntity_debeMapearSolicitudAEntidadActividad() {
        // ARRANGE: instanciar la comunidad contenedora y los datos del formulario DTO
        UUID createdBy = UUID.randomUUID();
        Community community = new Community(
                "Club Jazz",
                "Comunidad para fans del Jazz",
                CommunityType.MUSICA,
                CommunityVisibility.PUBLICA,
                createdBy
        );
        community.setId(UUID.randomUUID());

        OffsetDateTime startsAt = OffsetDateTime.now().plusDays(1);
        OffsetDateTime endsAt = startsAt.plusHours(2);

        ActivityCreateRequestDto dto = new ActivityCreateRequestDto(
                "Concierto en Vivo",
                "Transmisión especial de Jazz",
                "YouTube Live",
                startsAt,
                endsAt
        );

        // ACT: invocar el método toEntity del mapper
        CommunityActivity entity = activityMapper.toEntity(dto, community, createdBy);

        // ASSERT: validar la asignación de atributos y el estado por defecto PROGRAMADA
        assertAll(
                () -> assertEquals("Concierto en Vivo", entity.getTitle()),
                () -> assertEquals("Transmisión especial de Jazz", entity.getDescription()),
                () -> assertEquals("YouTube Live", entity.getLocation()),
                () -> assertEquals(startsAt, entity.getStartsAt()),
                () -> assertEquals(endsAt, entity.getEndsAt()),
                () -> assertEquals(community, entity.getCommunity()),
                () -> assertEquals(createdBy, entity.getCreatedBy()),
                () -> assertEquals(ActivityStatus.PROGRAMADA, entity.getStatus())
        );
    }

    @Test
    @DisplayName("toResponseDto: Debe transformar la entidad a DTO e incluir las métricas contextuales")
    void toResponseDto_debeMapearActividadYMetadatosADto() {
        // ARRANGE: instanciar CommunityActivity con su comunidad contenedora y métricas simuladas
        UUID createdBy = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        Community community = new Community(
                "Club de Lectura",
                "Espacio literario",
                CommunityType.LIBROS,
                CommunityVisibility.PUBLICA,
                createdBy
        );
        community.setId(UUID.randomUUID());

        CommunityActivity entity = new CommunityActivity(
                community,
                createdBy,
                "Debate de Libro",
                "Discusión del capítulo 1",
                "Sala Zoom",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1),
                ActivityStatus.PROGRAMADA
        );
        entity.setId(activityId);

        long participantCount = 12L;
        boolean isParticipating = true;

        // ACT: transformar la entidad a DTO de respuesta mediante el mapper
        ActivityResponseDto response = activityMapper.toResponseDto(entity, participantCount, isParticipating);

        // ASSERT: verificar los campos mapeados, ID de la comunidad y banderas de participación
        assertAll(
                () -> assertEquals(activityId, response.id()),
                () -> assertEquals(community.getId(), response.communityId()),
                () -> assertEquals(createdBy, response.createdBy()),
                () -> assertEquals("Debate de Libro", response.title()),
                () -> assertEquals("Sala Zoom", response.location()),
                () -> assertEquals(ActivityStatus.PROGRAMADA, response.status()),
                () -> assertEquals(participantCount, response.participantCount()),
                () -> assertTrue(response.isParticipating())
        );
    }

    @Test
    @DisplayName("toParticipantResponseDto: Debe mapear la entidad ActivityParticipant a su DTO de respuesta")
    void toParticipantResponseDto_debeMapearParticipanteADto() {
        // ARRANGE: instanciar la actividad, el participante y asignar valores mediante setters públicos
        UUID participantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        CommunityActivity activity = new CommunityActivity(
                null,
                userId,
                "Taller de Guitarra",
                "Práctica básica",
                "Presencial",
                now,
                now.plusHours(2),
                ActivityStatus.PROGRAMADA
        );
        activity.setId(UUID.randomUUID());

        ActivityParticipant participant = new ActivityParticipant(
                activity,
                userId,
                ParticipantStatus.CONFIRMADA
        );
        participant.setId(participantId);
        participant.setConfirmedAt(now);

        // ACT: ejecutar la transformación hacia DTO mediante el mapper
        ActivityParticipantResponseDto response = activityMapper.toParticipantResponseDto(participant);

        // ASSERT: confirmar la correcta extracción del ID de la actividad, usuario, estado y fecha
        assertAll(
                () -> assertEquals(participantId, response.id()),
                () -> assertEquals(activity.getId(), response.activityId()),
                () -> assertEquals(userId, response.userId()),
                () -> assertEquals(ParticipantStatus.CONFIRMADA, response.status()),
                () -> assertEquals(now, response.confirmedAt())
        );
    }
}
