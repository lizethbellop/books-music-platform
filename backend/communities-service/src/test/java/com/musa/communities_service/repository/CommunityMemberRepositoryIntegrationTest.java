package com.musa.communities_service.repository;

import com.musa.communities_service.entity.Community;
import com.musa.communities_service.entity.CommunityMember;
import com.musa.communities_service.enums.CommunityType;
import com.musa.communities_service.enums.CommunityVisibility;
import com.musa.communities_service.enums.MemberRole;
import com.musa.communities_service.enums.MemberStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@SpringBootTest
@Transactional
public class CommunityMemberRepositoryIntegrationTest {

    @Autowired
    private CommunityMemberRepository memberRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private EntityManager entityManager;

    private Community community;
    private UUID userId1;
    private UUID userId2;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();

        community = new Community(
                "Comunidad Test Miembros",
                "Descripción de prueba",
                CommunityType.LIBROS,
                CommunityVisibility.PUBLICA,
                userId1
        );
        communityRepository.saveAndFlush(community);
    }

    @Test
    @DisplayName("findByCommunityIdAndUserId & findByCommunityIdAndUserIdAndStatus: Debe recuperar membresías por usuario y estado")
    void debeBuscarMembresiaPorComunidadUsuarioYEstado() {
        CommunityMember member = new CommunityMember(community, userId1, MemberRole.MIEMBRO, MemberStatus.ACTIVO);
        memberRepository.saveAndFlush(member);

        entityManager.flush();
        entityManager.clear();

        Optional<CommunityMember> foundAnyStatus = memberRepository.findByCommunityIdAndUserId(community.getId(), userId1);
        Optional<CommunityMember> foundActive = memberRepository.findByCommunityIdAndUserIdAndStatus(community.getId(), userId1, MemberStatus.ACTIVO);
        Optional<CommunityMember> foundRetirado = memberRepository.findByCommunityIdAndUserIdAndStatus(community.getId(), userId1, MemberStatus.RETIRADO);

        assertAll(
                () -> assertTrue(foundAnyStatus.isPresent(), "Debe encontrar la membresía sin filtrar por estado"),
                () -> assertTrue(foundActive.isPresent(), "Debe encontrar la membresía activa"),
                () -> assertTrue(foundRetirado.isEmpty(), "No debe retornar resultados para el estado RETIRADO"),
                () -> assertEquals(userId1, foundActive.get().getUserId())
        );
    }

    @Test
    @DisplayName("existsBy...: Debe verificar la existencia de membresía por estado y por rol de administración")
    void debeValidarExistenciaDeMembresiaYRol() {
        CommunityMember adminMember = new CommunityMember(community, userId1, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO);
        memberRepository.saveAndFlush(adminMember);

        entityManager.flush();
        entityManager.clear();

        boolean existsActive = memberRepository.existsByCommunityIdAndUserIdAndStatus(community.getId(), userId1, MemberStatus.ACTIVO);
        boolean existsAdmin = memberRepository.existsByCommunityIdAndUserIdAndRoleAndStatus(community.getId(), userId1, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO);
        boolean existsRegularMemberRole = memberRepository.existsByCommunityIdAndUserIdAndRoleAndStatus(community.getId(), userId1, MemberRole.MIEMBRO, MemberStatus.ACTIVO);

        assertAll(
                () -> assertTrue(existsActive, "Debe retornar true para miembro activo"),
                () -> assertTrue(existsAdmin, "Debe retornar true al validar el rol ADMINISTRADOR"),
                () -> assertFalse(existsRegularMemberRole, "Debe retornar false si no posee el rol MIEMBRO")
        );
    }

    @Test
    @DisplayName("countBy...: Debe contabilizar miembros por estado y por combinación con rol")
    void debeContabilizarMiembrosPorEstadoYRol() {
        CommunityMember admin = new CommunityMember(community, userId1, MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO);
        CommunityMember regular = new CommunityMember(community, userId2, MemberRole.MIEMBRO, MemberStatus.ACTIVO);
        memberRepository.saveAndFlush(admin);
        memberRepository.saveAndFlush(regular);

        entityManager.flush();
        entityManager.clear();

        long totalActivos = memberRepository.countByCommunityIdAndStatus(community.getId(), MemberStatus.ACTIVO);
        long totalAdmins = memberRepository.countByCommunityIdAndRoleAndStatus(community.getId(), MemberRole.ADMINISTRADOR, MemberStatus.ACTIVO);
        long totalMiembrosRegulares = memberRepository.countByCommunityIdAndRoleAndStatus(community.getId(), MemberRole.MIEMBRO, MemberStatus.ACTIVO);

        assertAll(
                () -> assertEquals(2, totalActivos, "Total de miembros activos debe ser 2"),
                () -> assertEquals(1, totalAdmins, "Total de administradores debe ser 1"),
                () -> assertEquals(1, totalMiembrosRegulares, "Total de miembros regulares debe ser 1")
        );
    }
}
