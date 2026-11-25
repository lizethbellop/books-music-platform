package com.musa.communities_service.polls.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(
        name = "polls.events.enabled",
        havingValue = "true"
)
public class PollChangePublisher {

    private static final Logger log =
            LoggerFactory.getLogger(PollChangePublisher.class);

    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;

    private record Pending(
            UUID pollId,
            int attempts,
            UUID revision
    ) {
    }

    public PollChangePublisher(
            JdbcTemplate jdbc,
            @Qualifier("pollRabbitTemplate") RabbitTemplate rabbit
    ) {
        this.jdbc = jdbc;
        this.rabbit = rabbit;
    }

    @Scheduled(
            fixedDelayString = "${polls.events.interval-ms:500}",
            scheduler = "pollPublisherScheduler"
    )
    public void publish() {
        try {
            publishOne();
        } catch (RuntimeException exception) {
            log.warn(
                    "Cambios de encuestas pendientes: {}",
                    exception.getClass().getSimpleName()
            );
        }
    }

    private void publishOne() {
        var pending = jdbc.query("""
                SELECT poll_id, attempts, revision
                FROM community_poll_changes
                WHERE next_attempt_at <= clock_timestamp()
                ORDER BY next_attempt_at, poll_id
                LIMIT 1
                """,
                (row, index) -> new Pending(
                        row.getObject(1, UUID.class),
                        row.getInt(2),
                        row.getObject(3, UUID.class)
                )
        );

        if (pending.isEmpty()) {
            return;
        }

        Pending event = pending.getFirst();

        try {
            var properties = new MessageProperties();
            properties.setContentType("text/plain");

            var correlation = new CorrelationData(
                    UUID.randomUUID().toString()
            );

            var message = new Message(
                    event.pollId().toString()
                            .getBytes(StandardCharsets.UTF_8),
                    properties
            );

            rabbit.send("musa.polls", "", message, correlation);

            var confirmation = correlation.getFuture()
                    .get(2, TimeUnit.SECONDS);

            if (!confirmation.ack()) {
                throw new IllegalStateException(
                        "Evento sin confirmación"
                );
            }

            jdbc.update("""
                    DELETE FROM community_poll_changes
                    WHERE poll_id = ? AND revision = ?
                    """,
                    event.pollId(),
                    event.revision()
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Publicación interrumpida",
                    exception
            );

        } catch (Exception exception) {
            int delay = (int) Math.min(
                    60,
                    2L << Math.min(event.attempts(), 5)
            );

            jdbc.update("""
                    UPDATE community_poll_changes
                    SET attempts = LEAST(attempts + 1, 1000),
                        next_attempt_at =
                            clock_timestamp() + (? * INTERVAL '1 second')
                    WHERE poll_id = ? AND revision = ?
                    """,
                    delay,
                    event.pollId(),
                    event.revision()
            );

            log.warn(
                    "Actualización de encuesta pendiente; "
                            + "reintento en {} segundos",
                    delay
            );
        }
    }
}