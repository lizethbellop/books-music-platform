package com.musa.chat.messaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;
@Component
@ConditionalOnProperty(name = "notifications.events.enabled", havingValue = "true")
public class NotificationOutbox {
    private final JdbcTemplate jdbc;
    private final JsonMapper json = JsonMapper.builder().build();
    public NotificationOutbox(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String routingKey, String type, UUID recipient, UUID actor, UUID target) {
        if (recipient.equals(actor)) return;
        UUID eventId = UUID.randomUUID();
        String payload = json.writeValueAsString(new NotificationEvent(1, eventId, type, recipient, actor, target));
        jdbc.update("INSERT INTO public.notification_outbox (event_id, routing_key, payload) VALUES (?, ?, ?)",
                eventId, routingKey, payload);
    }
}
