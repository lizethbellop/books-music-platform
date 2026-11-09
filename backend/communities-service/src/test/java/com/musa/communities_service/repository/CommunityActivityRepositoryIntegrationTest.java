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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración para validar las consultas del repositorio CommunityActivityRepository.
 */
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

        // ARRANGE: crear y persistir la comunidad organizadora
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
        // ARRANGE: crear y persistir una actividad programada
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

        // ACT: realizar la búsqueda puntual por id con estado, y por id cruzado con comunidad
        Optional<CommunityActivity> foundByStatus = activityRepository.findByIdAndStatus(savedActivity.getId(), ActivityStatus.PROGRAMADA);
        Optional<CommunityActivity> foundByCommunity = activityRepository.findByIdAndCommunityIdAndStatus(savedActivity.getId(), community.getId(), ActivityStatus.PROGRAMADA);
        Optional<CommunityActivity> foundCancelled = activityRepository.findByIdAndStatus(savedActivity.getId(), ActivityStatus.CANCELADA);

        // ASSERT: verificar que se recupere la actividad programada y falle si el estado difiere
        assertAll(
                () -> assertTrue(foundByStatus.isPresent(), "Debe encontrar la actividad por ID y estado PROGRAMADA"),
                () -> assertTrue(foundByCommunity.isPresent(), "Debe encontrar la actividad validando el ID de comunidad"),
                () -> assertTrue(foundCancelled.isEmpty(), "No debe retornar resultados si se busca como CANCELADA"),
                () -> assertEquals("Taller de Composición", foundByStatus.get().getTitle())
        );
    }

    @Test
    @DisplayName("findByCommunityIdAndStatus & findByCommunityIdAndCreatedByAndStatus: Debe obtener actividades paginadas")
    void debeObtenerActividadesPaginadasPorComunidadYOrganizador() {
        // ARRANGE: persistir una actividad programada y otra cancelada
        OffsetDateTime now = OffsetDateTime.now();
        CommunityActivity act1 = new CommunityActivity(
                community, organizerId, "Ensayo Abierto", "Descripción", "Estudio 1",
                now.plusDays(2), now.plusDays(2).plusHours(1), ActivityStatus.PROGRAMADA
        );
        CommunityActivity act2 = new CommunityActivity(
                community, organizerId, "Jam Session Cancelada", "Descripción", "Bar Central",
                now.plusDays(3), now.plusDays(3).plusHours(2), ActivityStatus.CANCELADA
        );
        activityRepository.saveAndFlush(act1);
        activityRepository.saveAndFlush(act2);

        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // ACT: consultar actividades programadas de la comunidad y filtradas por el creador organizador
        Page<CommunityActivity> communityActivitiesPage = activityRepository.findByCommunityIdAndStatus(community.getId(), ActivityStatus.PROGRAMADA, pageable);
        Page<CommunityActivity> organizerActivitiesPage = activityRepository.findByCommunityIdAndCreatedByAndStatus(community.getId(), organizerId, ActivityStatus.PROGRAMADA, pageable);

        // ASSERT: comprobar la paginación y que ignore la actividad cancelada
        assertAll(
                () -> assertEquals(1, communityActivitiesPage.getTotalElements(), "Debe retornar solo 1 actividad programada"),
                () -> assertEquals("Ensayo Abierto", communityActivitiesPage.getContent().get(0).getTitle()),
                () -> assertEquals(1, organizerActivitiesPage.getTotalElements(), "Debe retornar 1 actividad programada para el organizador")
        );
    }
}
