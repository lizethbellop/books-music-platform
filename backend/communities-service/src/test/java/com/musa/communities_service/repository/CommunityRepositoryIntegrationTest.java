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

/**
 * Pruebas de integración para CommunityRepository.
 * Evalúa los 6 métodos declarados en la interfaz contra una base de datos de prueba.
 */
@Tag("integration")
@SpringBootTest
@Transactional
public class CommunityRepositoryIntegrationTest {

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID userId1;
    private UUID userId2;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("1. existsByNameIgnoreCaseAndStatus: Debe validar existencia ignorando mayúsculas/minúsculas y respetando el estado")
    void existsByNameIgnoreCaseAndStatus_debeValidarExistenciaYEstado() {
        // ARRANGE: Crear comunidad en estado ACTIVA
        Community community = new Community(
                "Club de Lectura Musa",
                "Comunidad sobre literatura clásica.",
                CommunityType.LIBROS,
                CommunityVisibility.PUBLICA,
                userId1
        );
        community.setStatus(CommunityStatus.ACTIVA);

        // ACT: Persistir en la BD, forzar el INSERT y limpiar el caché L1 de Hibernate
        communityRepository.saveAndFlush(community);
        entityManager.flush();
        entityManager.clear();

        // ACT: Consultar la existencia probando variaciones de mayúsculas y estados lógicos
        boolean existsUpper = communityRepository.existsByNameIgnoreCaseAndStatus("CLUB DE LECTURA MUSA", CommunityStatus.ACTIVA);
        boolean existsLower = communityRepository.existsByNameIgnoreCaseAndStatus("club de lectura musa", CommunityStatus.ACTIVA);
        boolean existsEliminada = communityRepository.existsByNameIgnoreCaseAndStatus("Club de Lectura Musa", CommunityStatus.ELIMINADA);

        // ASSERT: Confirmar que la consulta coincida sin importar el casing y filtre por estado
        assertAll(
                () -> assertTrue(existsUpper, "Debe retornar true cuando el nombre coincide en mayúsculas"),
                () -> assertTrue(existsLower, "Debe retornar true cuando el nombre coincide en minúsculas"),
                () -> assertFalse(existsEliminada, "Debe retornar false cuando se consulta por estado ELIMINADA")
        );
    }

    @Test
    @DisplayName("2. findByIdAndStatus: Debe retornar la comunidad cuando coincida el UUID y el estado lógico")
    void findByIdAndStatus_debeRetornarComunidadSiCoincideIdYEstado() {
        // ARRANGE: Preparar la entidad y guardarla
        Community community = new Community(
                "Cinefilos Musa",
                "Comunidad para debate de cine.",
                CommunityType.MIXTO,
                CommunityVisibility.PUBLICA,
                userId1
        );
        community.setStatus(CommunityStatus.ACTIVA);
        Community saved = communityRepository.saveAndFlush(community);

        // ACT: Sincronizar sentencias SQL con PostgreSQL y desalojar la entidad de la memoria L1
        entityManager.flush();
        entityManager.clear();

        // ACT: Buscar por ID existente con estado ACTIVA y con estado ELIMINADA
        Optional<Community> foundActive = communityRepository.findByIdAndStatus(saved.getId(), CommunityStatus.ACTIVA);
        Optional<Community> foundEliminada = communityRepository.findByIdAndStatus(saved.getId(), CommunityStatus.ELIMINADA);

        // ASSERT: Comprobar la presencia del resultado y la exactitud de los atributos recuperados
        assertAll(
                () -> assertTrue(foundActive.isPresent(), "Debe encontrar la comunidad activa por su ID"),
                () -> assertTrue(foundEliminada.isEmpty(), "No debe retornar nada si el estado no coincide"),
                () -> assertEquals("Cinefilos Musa", foundActive.get().getName()),
                () -> assertEquals(CommunityType.MIXTO, foundActive.get().getType())
        );
    }

    @Test
    @DisplayName("3. findByStatus: Debe retornar solo comunidades con el estado especificado de forma paginada")
    void findByStatus_debeRetornarComunidadesPaginadasPorEstado() {
        // ARRANGE: Insertar 2 comunidades ACTIVAS y 1 ELIMINADA
        Community com1 = new Community("Rock Clásico", "Música rock 70s y 80s", CommunityType.MUSICA, CommunityVisibility.PUBLICA, userId1);
        com1.setStatus(CommunityStatus.ACTIVA);

        Community com2 = new Community("Jazz & Blues", "Música jazz", CommunityType.MUSICA, CommunityVisibility.PUBLICA, userId1);
        com2.setStatus(CommunityStatus.ACTIVA);

        Community com3 = new Community("Comunidad Inactiva", "Comunidad obsoleta", CommunityType.MIXTO, CommunityVisibility.PRIVADA, userId1);
        com3.setStatus(CommunityStatus.ELIMINADA);

        communityRepository.saveAndFlush(com1);
        communityRepository.saveAndFlush(com2);
        communityRepository.saveAndFlush(com3);

        // ACT: Forzar escritura física y limpiar sesión JPA
        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // ACT: Consultar comunidades paginadas por estado ACTIVA
        Page<Community> activePage = communityRepository.findByStatus(CommunityStatus.ACTIVA, pageable);

        // ASSERT: Validar que solo se recuperen los registros en estado ACTIVA
        assertAll(
                () -> assertEquals(2, activePage.getTotalElements(), "Debe recuperar exactamente 2 comunidades activas"),
                () -> assertTrue(activePage.getContent().stream().allMatch(c -> c.getStatus() == CommunityStatus.ACTIVA))
        );
    }

    @Test
    @DisplayName("4. findByTypeAndStatus: Debe filtrar por el tipo especificado y estado ACTIVA")
    void findByTypeAndStatus_debeFiltrarPorTipoYEstado() {
        // ARRANGE: Guardar comunidades de tipo LIBROS y de tipo MUSICA
        Community librosCom = new Community("Club Novela Negra", "Libros de misterio", CommunityType.LIBROS, CommunityVisibility.PUBLICA, userId1);
        librosCom.setStatus(CommunityStatus.ACTIVA);

        Community musicaCom = new Community("Heavy Metal Fans", "Heavy metal universal", CommunityType.MUSICA, CommunityVisibility.PUBLICA, userId1);
        musicaCom.setStatus(CommunityStatus.ACTIVA);

        communityRepository.saveAndFlush(librosCom);
        communityRepository.saveAndFlush(musicaCom);

        // ACT: Enviar SQL a PostgreSQL y limpiar contexto de persistencia
        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // ACT: Buscar únicamente comunidades de tipo LIBROS y estado ACTIVA
        Page<Community> librosPage = communityRepository.findByTypeAndStatus(CommunityType.LIBROS, CommunityStatus.ACTIVA, pageable);

        // ASSERT: Verificar filtrado estricto por tipo
        assertAll(
                () -> assertEquals(1, librosPage.getTotalElements(), "Debe encontrar 1 sola comunidad de tipo LIBROS"),
                () -> assertEquals("Club Novela Negra", librosPage.getContent().get(0).getName()),
                () -> assertEquals(CommunityType.LIBROS, librosPage.getContent().get(0).getType())
        );
    }

    @Test
    @DisplayName("5. findByCreatedByAndStatus: Debe filtrar comunidades según el UUID del usuario creador")
    void findByCreatedByAndStatus_debeFiltrarPorCreadorYEstado() {
        // ARRANGE: Crear comunidades asociadas a dos usuarios diferentes (userId1 y userId2)
        Community user1Com = new Community("Grupo de Usuario 1", "Creado por user1", CommunityType.MIXTO, CommunityVisibility.PUBLICA, userId1);
        user1Com.setStatus(CommunityStatus.ACTIVA);

        Community user2Com = new Community("Grupo de Usuario 2", "Creado por user2", CommunityType.MIXTO, CommunityVisibility.PUBLICA, userId2);
        user2Com.setStatus(CommunityStatus.ACTIVA);

        communityRepository.saveAndFlush(user1Com);
        communityRepository.saveAndFlush(user2Com);

        // ACT: Sincronizar y desvincular proxies JPA
        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // ACT: Consultar comunidades creadas por userId1
        Page<Community> user1Page = communityRepository.findByCreatedByAndStatus(userId1, CommunityStatus.ACTIVA, pageable);

        // ASSERT: Validar la pertenencia por creador
        assertAll(
                () -> assertEquals(1, user1Page.getTotalElements(), "Debe retornar 1 comunidad creada por userId1"),
                () -> assertEquals(userId1, user1Page.getContent().get(0).getCreatedBy()),
                () -> assertEquals("Grupo de Usuario 1", user1Page.getContent().get(0).getName())
        );
    }

    @Test
    @DisplayName("6. searchCommunities: Debe ejecutar la consulta JPQL con coincidencias de texto y tipo opcional")
    void searchCommunities_debeProbarBusquedaAvanzadaJPQL() {
        // ARRANGE: Crear escenarios variados (búsqueda parcial en nombre y descripción)
        Community com1 = new Community("Fantasía Épica", "Debate de sagas de libros", CommunityType.LIBROS, CommunityVisibility.PUBLICA, userId1);
        com1.setStatus(CommunityStatus.ACTIVA);

        Community com2 = new Community("Música Sinfónica", "Conciertos de música épica", CommunityType.MUSICA, CommunityVisibility.PUBLICA, userId1);
        com2.setStatus(CommunityStatus.ACTIVA);

        Community com3 = new Community("Poesía Clásica", "Análisis poético", CommunityType.LIBROS, CommunityVisibility.PUBLICA, userId1);
        com3.setStatus(CommunityStatus.ACTIVA);

        communityRepository.saveAndFlush(com1);
        communityRepository.saveAndFlush(com2);
        communityRepository.saveAndFlush(com3);

        // ACT: Forzar vaciado de memoria para obligar la ejecución del SELECT JPQL
        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // ACT 1: Buscar la palabra 'épica' sin filtro de tipo (query="épica", type=null)
        Page<Community> textSearch = communityRepository.searchCommunities("épica", null, CommunityStatus.ACTIVA, pageable);

        // ACT 2: Buscar la palabra 'épica' filtrando por tipo LIBROS (query="épica", type=LIBROS)
        Page<Community> textAndTypeSearch = communityRepository.searchCommunities("épica", CommunityType.LIBROS, CommunityStatus.ACTIVA, pageable);

        // ACT 3: Búsqueda sin filtros de texto ni tipo (query=null, type=null) -> retorna todas las activas
        Page<Community> allActiveSearch = communityRepository.searchCommunities(null, null, CommunityStatus.ACTIVA, pageable);

        // ASSERT: Validar comportamiento dinámico de la cláusula WHERE en JPQL
        assertAll(
                () -> assertEquals(2, textSearch.getTotalElements(), "Debe encontrar 2 comunidades con la palabra 'épica' (en nombre o descripción)"),
                () -> assertEquals(1, textAndTypeSearch.getTotalElements(), "Debe encontrar solo 1 comunidad con la palabra 'épica' que sea de tipo LIBROS"),
                () -> assertEquals("Fantasía Épica", textAndTypeSearch.getContent().get(0).getName()),
                () -> assertEquals(3, allActiveSearch.getTotalElements(), "Debe retornar las 3 comunidades activas cuando los filtros opcionales son null")
        );
    }
}