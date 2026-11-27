package com.musa.social;
import com.musa.social.service.FollowService;
import com.musa.social.messaging.NotificationOutboxPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@EnabledIfEnvironmentVariable(named="NOTIFICATIONS_TEST_ISOLATED", matches="true")
class SocialOutboxIntegrationTest {
    @Autowired FollowService follows;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean NotificationOutboxPublisher publisher;
    @DynamicPropertySource static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("SOCIAL_OUTBOX_TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> "integration_owner");
        registry.add("spring.datasource.password", () -> System.getenv("NOTIFICATIONS_TEST_DB_PASSWORD"));
        registry.add("musa.security.jwt.secret", () -> Base64.getEncoder().encodeToString(new byte[64]));
        registry.add("notifications.events.enabled", () -> "true");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.config.import", () -> "");
    }
    @Test void followCommitsEventAndDuplicateFollowDoesNotCreateAnother() {
        UUID actor=UUID.randomUUID(), recipient=UUID.randomUUID(); follows.followUser(actor, recipient);
        String payload=jdbc.queryForObject("SELECT payload FROM notification_outbox WHERE payload LIKE ?", String.class, "%"+recipient+"%");
        assertTrue(payload.contains("USER_FOLLOWED")); assertTrue(payload.contains(actor.toString()));
        assertThrows(RuntimeException.class, () -> follows.followUser(actor, recipient));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE payload LIKE ?", Integer.class, "%"+recipient+"%"));
    }
}
