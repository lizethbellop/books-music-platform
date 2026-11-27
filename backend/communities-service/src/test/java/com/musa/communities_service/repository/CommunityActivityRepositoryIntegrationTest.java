package com.musa.communities_service.repository;

import com.musa.communities_service.entity.Community;
import com.musa.communities_service.entity.CommunityActivity;
import com.musa.communities_service.enums.ActivityStatus;
import com.musa.communities_service.enums.CommunityType;
import com.musa.communities_service.enums.CommunityVisibility;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@SpringBootTest
@Transactional
public class CommunityActivityRepositoryIntegrationTest {

    @Autowired
    private CommunityActivityRepository activityRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private EntityManager entityManager;

    private Community community;
    private UUID organizerId;

    @BeforeEach
    void setUp() {
        organizerId = UUID.randomUUID();

        community = new Community(
                "Comunidad Eventos Test",
                "Descripción de prueba",
                CommunityType.MUSICA,
                CommunityVisibility.PUBLICA,
                organizerId
        );
        communityRepository.saveAndFlush(community);
    }

    @Test
    @DisplayName("findByIdAndStatus & findByIdAndCommunityIdAndStatus: Debe recuperar actividades por ID, comunidad y estado")
    void debeBuscarActividadPorIdComunidadYEstado() {
        OffsetDateTime now = OffsetDateTime.now();
        CommunityActivity activity = new CommunityActivity(
                community,
                organizerId,
                "Taller de Composición",
                "Aprende bases musicales",
                "Sala Virtual Zoom",
                now.plusDays(1),
                now.plusDays(1).plusHours(2),
                ActivityStatus.PROGRAMADA
        );
        CommunityActivity savedActivity = activityRepository.saveAndFlush(activity);

        entityManager.flush();
        entityManager.clear();

        Optional<CommunityActivity> foundByStatus = activityRepository.findByIdAndStatus(savedActivity.getId(), ActivityStatus.PROGRAMADA);
        Optional<CommunityActivity> foundByCommunity = activityRepository.findByIdAndCommunityIdAndStatus(savedActivity.getId(), community.getId(), ActivityStatus.PROGRAMADA);
        Optional<CommunityActivity> foundCancelled = activityRepository.findByIdAndStatus(savedActivity.getId(), ActivityStatus.CANCELADA);

        assertAll(
                () -> assertTrue(foundByStatus.isPresent(), "Debe encontrar la actividad por ID y estado PROGRAMADA"),
                () -> assertTrue(foundByCommunity.isPresent(), "Debe encontrar la actividad validando el ID de comunidad"),
                () -> assertTrue(foundCancelled.isEmpty(), "No debe retornar resultados si se busca como CANCELADA"),
                () -> assertEquals("Taller de Composición", foundByStatus.get().getTitle())
        );
    }
}