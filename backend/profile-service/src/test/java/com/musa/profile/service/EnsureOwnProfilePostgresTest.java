package com.musa.profile.service;

import com.musa.profile.entity.Profile;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.config.import=optional:file:.env[.properties]",
        "spring.datasource.url=${PROFILE_DB_URL}",
        "spring.datasource.username=${PROFILE_DB_USERNAME}",
        "spring.datasource.password=${PROFILE_DB_PASSWORD}",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@EnabledIfEnvironmentVariable(
        named = "MUSA_PROFILE_DB_TEST",
        matches = "true"
)
@Transactional
class EnsureOwnProfilePostgresTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void debeCrearUnSoloPerfilYConservarSusDatos() {
        UUID userId = UUID.randomUUID();

        Profile first = profileService.ensureOwnProfile(userId);
        UUID profileId = first.getId();

        assertNotNull(profileId);

        first.setBiography("Mi biografía de prueba");
        first.setPrivateProfile(true);

        entityManager.flush();
        entityManager.clear();

        Profile second = profileService.ensureOwnProfile(userId);

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM profiles WHERE user_id = ?",
                Long.class,
                userId
        );

        assertAll(
                () -> assertEquals(profileId, second.getId()),
                () -> assertEquals(
                        "Mi biografía de prueba",
                        second.getBiography()
                ),
                () -> assertTrue(second.isPrivateProfile()),
                () -> assertEquals(Long.valueOf(1), count)
        );
    }
}