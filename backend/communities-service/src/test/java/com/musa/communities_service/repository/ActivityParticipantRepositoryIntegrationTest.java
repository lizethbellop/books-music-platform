package com.musa.communities_service.repository;

import com.musa.communities_service.entity.ActivityParticipant;
import com.musa.communities_service.entity.Community;
import com.musa.communities_service.entity.CommunityActivity;
import com.musa.communities_service.enums.ActivityStatus;
import com.musa.communities_service.enums.CommunityType;
import com.musa.communities_service.enums.CommunityVisibility;
import com.musa.communities_service.enums.ParticipantStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración para validar las consultas del repositorio ActivityParticipantRepository.
 */
@Tag("integration")
@SpringBootTest
@Transactional
public class ActivityParticipantRepositoryIntegrationTest {

    @Autowired
    private ActivityParticipantRepository participantRepository;

    @Autowired
    private CommunityActivityRepository activityRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private EntityManager entityManager;

    private CommunityActivity activity;
    private UUID participantUserId;

    @BeforeEach
    void setUp() {
        UUID organizerId = UUID.randomUUID();
        participantUserId = UUID.randomUUID();

        // ARRANGE: preparar la jerarquía de entidades requeridas (Community -> CommunityActivity)
        Community community = new Community(
                "Comunidad RSVP Test",
                "Descripción",
                CommunityType.MIXTO,
                CommunityVisibility.PUBLICA,
                organizerId
        );
        communityRepository.saveAndFlush(community);

        OffsetDateTime now = OffsetDateTime.now();
        activity = new CommunityActivity(
                community,
                organizerId,
                "Debate Literario RSVP",
                "Descripción evento",
                "Auditorio Principal",
                now.plusDays(1),
                now.plusDays(1).plusHours(2),
                ActivityStatus.PROGRAMADA
        );
        activityRepository.saveAndFlush(activity);
    }

    @Test
    @DisplayName("findByActivityIdAndUserId & existsBy...: Debe consultar y verificar la confirmación RSVP")
    void debeBuscarYVerificarExistenciaDeParticipante() {
        // ARRANGE: registrar la participación confirmada del usuario en la actividad
        ActivityParticipant participant = new ActivityParticipant(activity, participantUserId, ParticipantStatus.CONFIRMADA);
        participantRepository.saveAndFlush(participant);

        entityManager.flush();
        entityManager.clear();

        // ACT: consultar la inscripción y validar su existencia por estado CONFIRMADA
        Optional<ActivityParticipant> found = participantRepository.findByActivityIdAndUserId(activity.getId(), participantUserId);
        boolean existsConfirmed = participantRepository.existsByActivityIdAndUserIdAndStatus(activity.getId(), participantUserId, ParticipantStatus.CONFIRMADA);
        boolean existsCancelled = participantRepository.existsByActivityIdAndUserIdAndStatus(activity.getId(), participantUserId, ParticipantStatus.CANCELADA);

        // ASSERT: validar el hallazgo del registro y la coherencia del indicador booleano
        assertAll(
                () -> assertTrue(found.isPresent(), "Debe encontrar el registro de participación"),
                () -> assertTrue(existsConfirmed, "Debe retornar true para asistencia CONFIRMADA"),
                () -> assertFalse(existsCancelled, "Debe retornar false para estado CANCELADA"),
                () -> assertEquals(ParticipantStatus.CONFIRMADA, found.get().getStatus())
        );
    }

    @Test
    @DisplayName("findByActivityIdAndStatus & findByUserIdAndStatus & countBy...: Debe listar y contar los asistentes paginados")
    void debeObtenerParticipantesPaginadosYContabilizarConfirmados() {
        // ARRANGE: inscribir 2 usuarios confirmados en la actividad
        UUID secondUserId = UUID.randomUUID();
        ActivityParticipant part1 = new ActivityParticipant(activity, participantUserId, ParticipantStatus.CONFIRMADA);
        ActivityParticipant part2 = new ActivityParticipant(activity, secondUserId, ParticipantStatus.CONFIRMADA);
        participantRepository.saveAndFlush(part1);
        participantRepository.saveAndFlush(part2);

        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // ACT: obtener páginas por actividad y por usuario, e invocar el método de conteo
        Page<ActivityParticipant> activityParticipantsPage = participantRepository.findByActivityIdAndStatus(activity.getId(), ParticipantStatus.CONFIRMADA, pageable);
        Page<ActivityParticipant> userHistoryPage = participantRepository.findByUserIdAndStatus(participantUserId, ParticipantStatus.CONFIRMADA, pageable);
        long confirmedCount = participantRepository.countByActivityIdAndStatus(activity.getId(), ParticipantStatus.CONFIRMADA);

        // ASSERT: verificar totales y contenido paginado
        assertAll(
                () -> assertEquals(2, activityParticipantsPage.getTotalElements(), "Debe listar 2 participantes confirmados en la actividad"),
                () -> assertEquals(1, userHistoryPage.getTotalElements(), "Debe listar 1 actividad en el historial del usuario"),
                () -> assertEquals(2L, confirmedCount, "El contador de asistentes confirmados debe ser 2")
        );
    }
}
