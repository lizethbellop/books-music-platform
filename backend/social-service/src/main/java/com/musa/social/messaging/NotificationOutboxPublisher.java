package com.musa.social.messaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "notifications.events.enabled", havingValue = "true")
public class NotificationOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxPublisher.class);
    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;
    private final TransactionTemplate transactions;
    private record Pending(UUID eventId, String routingKey, String payload, int attempts) {}
    public NotificationOutboxPublisher(JdbcTemplate jdbc, RabbitTemplate rabbit, PlatformTransactionManager manager) {
        this.jdbc = jdbc; this.rabbit = rabbit; this.transactions = new TransactionTemplate(manager);
    }
    @Scheduled(fixedDelayString = "${notifications.events.poll-delay-ms:2000}")
    public void publishPending() {
        try { transactions.executeWithoutResult(status -> publishBatch()); }
        catch (RuntimeException exception) {
            log.warn("No se pudo revisar la outbox: {}", exception.getClass().getSimpleName());
        }
    }
    private void publishBatch() {
        var pending = jdbc.query("""
            SELECT event_id, routing_key, payload, attempts FROM public.notification_outbox
            WHERE published_at IS NULL AND next_attempt_at <= CURRENT_TIMESTAMP
            ORDER BY created_at, event_id LIMIT 8 FOR UPDATE SKIP LOCKED
            """, (row, index) -> new Pending(row.getObject("event_id", UUID.class), row.getString("routing_key"),
                    row.getString("payload"), row.getInt("attempts")));
        for (Pending event : pending) {
            try {
                MessageProperties properties = new MessageProperties();
                properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                properties.setContentEncoding("UTF-8");
                properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                properties.setMessageId(event.eventId().toString());
                CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
                rabbit.send("musa.events", event.routingKey(),
                        new Message(event.payload().getBytes(StandardCharsets.UTF_8), properties), correlation);
                var confirmation = correlation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirmation.ack() || correlation.getReturned() != null)
                    throw new IllegalStateException("Publicación no confirmada o sin ruta.");
                jdbc.update("UPDATE public.notification_outbox SET published_at = CURRENT_TIMESTAMP WHERE event_id = ?", event.eventId());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Publicación interrumpida", exception);
            } catch (Exception exception) {
                int delay = (int) Math.min(300, 2L << Math.min(event.attempts(), 7));
                jdbc.update("""
                    UPDATE public.notification_outbox SET attempts = attempts + 1,
                        next_attempt_at = CURRENT_TIMESTAMP + (? * INTERVAL '1 second')
                    WHERE event_id = ?
                    """, delay, event.eventId());
                log.warn("Evento {} pendiente; nuevo intento en {} segundos ({})", event.eventId(), delay,
                        exception.getClass().getSimpleName());
                // Evitar bloquear la BD por varios tiempos de espera si el broker está caído.
                break;
            }
        }
    }
}
