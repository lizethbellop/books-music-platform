package com.musa.chat;
import com.musa.chat.client.UsersClient;
import com.musa.chat.dto.request.*;
import com.musa.chat.service.*;
import com.musa.chat.messaging.NotificationOutboxPublisher;
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
class ChatOutboxIntegrationTest {
    @Autowired ConversationService conversations;
    @Autowired ChatMessagingService messages;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean UsersClient users;
    @MockitoBean NotificationOutboxPublisher publisher;
    @DynamicPropertySource static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("CHAT_OUTBOX_TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> "integration_owner");
        registry.add("spring.datasource.password", () -> System.getenv("NOTIFICATIONS_TEST_DB_PASSWORD"));
        registry.add("musa.security.jwt.secret", () -> Base64.getEncoder().encodeToString(new byte[64]));
        registry.add("notifications.events.enabled", () -> "true");
        registry.add("spring.config.import", () -> "");
    }
    @Test void sendingMessageCommitsPendingEventForOtherParticipantOnly() {
        UUID sender = UUID.randomUUID(), recipient = UUID.randomUUID();
        var c = conversations.createPrivateConversation(sender, new CreatePrivateConversationRequest(recipient));
        messages.send(sender, c.id(), new SendMessageRequest("mensaje privado"));
        String payload = jdbc.queryForObject("SELECT payload FROM notification_outbox WHERE payload LIKE ?", String.class, "%" + c.id() + "%");
        assertTrue(payload.contains(recipient.toString()));
        assertTrue(payload.contains("CHAT_MESSAGE_CREATED"));
        assertFalse(payload.contains("mensaje privado"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE payload LIKE ?", Integer.class, "%" + c.id() + "%"));
    }
}
