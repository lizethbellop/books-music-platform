package com.musa.communities_service.mapper;

import com.musa.communities_service.dto.request.CommunityCreateRequestDto;
import com.musa.communities_service.dto.response.CommunityMemberResponseDto;
import com.musa.communities_service.dto.response.CommunityResponseDto;
import com.musa.communities_service.entity.Community;
import com.musa.communities_service.entity.CommunityMember;
import com.musa.communities_service.enums.CommunityStatus;
import com.musa.communities_service.enums.CommunityType;
import com.musa.communities_service.enums.CommunityVisibility;
import com.musa.communities_service.enums.MemberRole;
import com.musa.communities_service.enums.MemberStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar las transformaciones de objetos en CommunityMapper.
 */
class CommunityMapperTest {

    private CommunityMapper communityMapper;

    @BeforeEach
    void setUp() {
        communityMapper = new CommunityMapper();
    }

    @Test
    @DisplayName("toEntity: Debe transformar un DTO de creación en una Entidad con los datos del creador")
    void toEntity_debeTransformarDtoAEntidadCorrectamente() {
        // ARRANGE (Preparación): crear un DTO de solicitud de comunidad y un UUID de usuario creador.
        UUID createdBy = UUID.randomUUID();
        CommunityCreateRequestDto dto = new CommunityCreateRequestDto(
                "Melómanos Musa",
                "Comunidad sobre apreciación musical.",
                CommunityType.MUSICA,
                CommunityVisibility.PUBLICA
        );

        // ACT (Ejecución): invocar el método toEntity del mapper.
        Community entity = communityMapper.toEntity(dto, createdBy);

        // ASSERT (Verificación): validar que los campos de la entidad contengan los valores exactos especificados.
        assertAll(
                () -> assertEquals("Melómanos Musa", entity.getName()),
                () -> assertEquals("Comunidad sobre apreciación musical.", entity.getDescription()),
                () -> assertEquals(CommunityType.MUSICA, entity.getType()),
                () -> assertEquals(CommunityVisibility.PUBLICA, entity.getVisibility()),
                () -> assertEquals(createdBy, entity.getCreatedBy()),
                () -> assertEquals(CommunityStatus.ACTIVA, entity.getStatus())
        );
    }

    @Test
    @DisplayName("toResponseDto: Debe transformar una Entidad a DTO integrando las métricas contextuales")
    void toResponseDto_debeMapearEntidadYCamposCalculados() {
        // ARRANGE (Preparación): instanciar una entidad Community y preparar datos contextuales calculados.
        UUID createdBy = UUID.randomUUID();
        Community entity = new Community(
                "Club de Literatura",
                "Espacio de debate",
                CommunityType.LIBROS,
                CommunityVisibility.PUBLICA,
                createdBy
        );
        entity.setId(UUID.randomUUID());

        long memberCount = 25L;
        boolean isMember = true;
        MemberRole userRole = MemberRole.ADMINISTRADOR;

        // ACT (Ejecución): Convertir la entidad a DTO de respuesta con los campos dinámicos.
        CommunityResponseDto response = communityMapper.toResponseDto(entity, memberCount, isMember, userRole);

        // ASSERT (Verificación): validar la copia de datos persistidos y la inclusión de los cálculos contextuales.
        assertAll(
                () -> assertEquals(entity.getId(), response.id()),
                () -> assertEquals("Club de Literatura", response.name()),
                () -> assertEquals(CommunityType.LIBROS, response.type()),
                () -> assertEquals(memberCount, response.memberCount()),
                () -> assertTrue(response.isMember()),
                () -> assertEquals(MemberRole.ADMINISTRADOR, response.userRole())
        );
    }

    @Test
    @DisplayName("toMemberResponseDto: Debe transformar una entidad CommunityMember a DTO de respuesta")
    void toMemberResponseDto_debeMapearEntidadMiembroADto() {
        // ARRANGE (Preparación): nistanciar la comunidad contenedora usando su constructor público
        UUID memberId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Community community = new Community("Cine", "Cine", CommunityType.MIXTO, CommunityVisibility.PUBLICA, userId);
        community.setId(UUID.randomUUID());

        // instanciar CommunityMember usando su constructor público de negocio
        CommunityMember member = new CommunityMember(
                community,
                userId,
                MemberRole.MIEMBRO,
                MemberStatus.ACTIVO
        );

        // asignar los valores requeridos para el DTO mediante sus setters públicos
        member.setId(memberId);
        member.setJoinedAt(now);

        // ACT (Ejecución): transformar la entidad a DTO mediante el mapper
        CommunityMemberResponseDto response = communityMapper.toMemberResponseDto(member);

        //ASSERT (Verificación): Confirmar la extracción correcta del ID de comunidad, rol, fecha y estado
        assertAll(
                () -> assertEquals(memberId, response.id()),
                () -> assertEquals(community.getId(), response.communityId()),
                () -> assertEquals(userId, response.userId()),
                () -> assertEquals(MemberRole.MIEMBRO, response.role()),
                () -> assertEquals(now, response.joinedAt()),
                () -> assertEquals(MemberStatus.ACTIVO, response.status())
        );
    }
}