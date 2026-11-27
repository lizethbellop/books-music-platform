package com.musa.communities_service.repository;

import com.musa.communities_service.entity.Community;
import com.musa.communities_service.enums.CommunityStatus;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@SpringBootTest
@Transactional
public class CommunityRepositoryIntegrationTest {

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID userId1;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
    }

    @Test
    @DisplayName("1. existsByNameIgnoreCaseAndStatus: Debe validar existencia ignorando mayúsculas/minúsculas y respetando el estado")
    void existsByNameIgnoreCaseAndStatus_debeValidarExistenciaYEstado() {
        Community community = new Community(
                "Club de Lectura Musa",
                "Comunidad sobre literatura clásica.",
                CommunityType.LIBROS,
                CommunityVisibility.PUBLICA,
                userId1
        );
        community.setStatus(CommunityStatus.ACTIVA);

        communityRepository.saveAndFlush(community);
        entityManager.flush();
        entityManager.clear();

        boolean existsUpper = communityRepository.existsByNameIgnoreCaseAndStatus("CLUB DE LECTURA MUSA", CommunityStatus.ACTIVA);
        boolean existsLower = communityRepository.existsByNameIgnoreCaseAndStatus("club de lectura musa", CommunityStatus.ACTIVA);
        boolean existsEliminada = communityRepository.existsByNameIgnoreCaseAndStatus("Club de Lectura Musa", CommunityStatus.ELIMINADA);

        assertAll(
                () -> assertTrue(existsUpper, "Debe retornar true cuando el nombre coincide en mayúsculas"),
                () -> assertTrue(existsLower, "Debe retornar true cuando el nombre coincide en minúsculas"),
                () -> assertFalse(existsEliminada, "Debe retornar false cuando se consulta por estado ELIMINADA")
        );
    }

    @Test
    @DisplayName("2. findByIdAndStatus: Debe retornar la comunidad cuando coincida el UUID y el estado lógico")
    void findByIdAndStatus_debeRetornarComunidadSiCoincideIdYEstado() {
        Community community = new Community(
                "Cinefilos Musa",
                "Comunidad para debate de cine.",
                CommunityType.MIXTO,
                CommunityVisibility.PUBLICA,
                userId1
        );
        community.setStatus(CommunityStatus.ACTIVA);
        Community saved = communityRepository.saveAndFlush(community);

        entityManager.flush();
        entityManager.clear();

        Optional<Community> foundActive = communityRepository.findByIdAndStatus(saved.getId(), CommunityStatus.ACTIVA);
        Optional<Community> foundEliminada = communityRepository.findByIdAndStatus(saved.getId(), CommunityStatus.ELIMINADA);

        assertAll(
                () -> assertTrue(foundActive.isPresent(), "Debe encontrar la comunidad activa por su ID"),
                () -> assertTrue(foundEliminada.isEmpty(), "No debe retornar nada si el estado no coincide"),
                () -> assertEquals("Cinefilos Musa", foundActive.get().getName()),
                () -> assertEquals(CommunityType.MIXTO, foundActive.get().getType())
        );
    }

    @Test
    @DisplayName("3. searchCommunities: Debe ejecutar la consulta JPQL con coincidencias de texto y tipo opcional")
    void searchCommunities_debeProbarBusquedaAvanzadaJPQL() {
        Community com1 = new Community("Fantasía Épica", "Debate de sagas de libros", CommunityType.LIBROS, CommunityVisibility.PUBLICA, userId1);
        com1.setStatus(CommunityStatus.ACTIVA);

        Community com2 = new Community("Música Sinfónica", "Conciertos de música épica", CommunityType.MUSICA, CommunityVisibility.PUBLICA, userId1);
        com2.setStatus(CommunityStatus.ACTIVA);

        Community com3 = new Community("Poesía Clásica", "Análisis poético", CommunityType.LIBROS, CommunityVisibility.PUBLICA, userId1);
        com3.setStatus(CommunityStatus.ACTIVA);

        communityRepository.saveAndFlush(com1);
        communityRepository.saveAndFlush(com2);
        communityRepository.saveAndFlush(com3);

        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        Page<Community> textSearch = communityRepository.searchCommunities("épica", null, CommunityStatus.ACTIVA, pageable);
        Page<Community> textAndTypeSearch = communityRepository.searchCommunities("épica", CommunityType.LIBROS, CommunityStatus.ACTIVA, pageable);
        Page<Community> allActiveSearch = communityRepository.searchCommunities(null, null, CommunityStatus.ACTIVA, pageable);

        assertAll(
                () -> assertEquals(2, textSearch.getTotalElements(), "Debe encontrar 2 comunidades con la palabra 'épica'"),
                () -> assertEquals(1, textAndTypeSearch.getTotalElements(), "Debe encontrar solo 1 comunidad con 'épica' de tipo LIBROS"),
                () -> assertEquals("Fantasía Épica", textAndTypeSearch.getContent().get(0).getName()),
                () -> assertEquals(3, allActiveSearch.getTotalElements(), "Debe retornar las 3 comunidades activas")
        );
    }
}