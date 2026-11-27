package com.musa.chat.messaging;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@EnabledIfEnvironmentVariable(named = "NOTIFICATIONS_TEST_ISOLATED", matches = "true")
class NotificationOutboxTest {
    JdbcTemplate jdbc;
    DataSourceTransactionManager manager;
    @BeforeEach void setup() {
        var datasource = new DriverManagerDataSource(System.getenv("OUTBOX_TEST_DB_URL"), "integration_owner", System.getenv("NOTIFICATIONS_TEST_DB_PASSWORD"));
        jdbc = new JdbcTemplate(datasource); manager = new DataSourceTransactionManager(datasource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS outbox_probe (id UUID PRIMARY KEY)");
        jdbc.update("UPDATE notification_outbox SET published_at = CURRENT_TIMESTAMP WHERE published_at IS NULL");
    }
    @Test void businessAndEventRollbackTogether() {
        UUID probe = UUID.randomUUID();
        var outbox = new NotificationOutbox(jdbc);
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(manager).executeWithoutResult(status -> {
            jdbc.update("INSERT INTO outbox_probe (id) VALUES (?)", probe);
            outbox.enqueue("test.event", "POST_LIKED", UUID.randomUUID(), UUID.randomUUID(), probe);
            throw new IllegalStateException("rollback");
        }));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM outbox_probe WHERE id = ?", Integer.class, probe));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE payload LIKE ?", Integer.class, "%" + probe + "%"));
    }
    @Test void failedPublishStaysPendingAndSuccessfulRetryKeepsEventId() {
        UUID marker = UUID.randomUUID();
        var outbox = new NotificationOutbox(jdbc);
        new TransactionTemplate(manager).executeWithoutResult(status -> outbox.enqueue("social.post.liked", "POST_LIKED",
                UUID.randomUUID(), UUID.randomUUID(), marker));
        UUID id = jdbc.queryForObject("SELECT event_id FROM notification_outbox WHERE payload LIKE ?", UUID.class, "%" + marker + "%");
        var rabbit = mock(RabbitTemplate.class);
        doThrow(new AmqpConnectException(new java.net.ConnectException("offline"))).when(rabbit)
                .send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        var publisher = new NotificationOutboxPublisher(jdbc, rabbit, manager); publisher.publishPending();
        assertNull(jdbc.queryForObject("SELECT published_at FROM notification_outbox WHERE event_id = ?", Object.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT attempts FROM notification_outbox WHERE event_id = ?", Integer.class, id));
        jdbc.update("UPDATE notification_outbox SET next_attempt_at = CURRENT_TIMESTAMP WHERE event_id = ?", id);
        reset(rabbit);
        doAnswer(invocation -> {
            Message message = invocation.getArgument(2); CorrelationData correlation = invocation.getArgument(3);
            assertEquals(id.toString(), message.getMessageProperties().getMessageId());
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null)); return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        publisher.publishPending();
        assertNotNull(jdbc.queryForObject("SELECT published_at FROM notification_outbox WHERE event_id = ?", Object.class, id));
    }
}
