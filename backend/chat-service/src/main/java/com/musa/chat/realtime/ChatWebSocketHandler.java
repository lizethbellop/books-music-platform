package com.musa.chat.realtime;

import com.musa.chat.service.MessageCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private final ChatTicketService tickets;
    private final JsonMapper json = JsonMapper.builder().build();
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    private static final class Connection {
        final WebSocketSession session;
        final Instant opened = Instant.now();
        volatile ChatTicketService.TicketIdentity identity;
        Connection(WebSocketSession session) {
            this.session = new ConcurrentWebSocketSessionDecorator(session, 5000, 65536);
        }
    }
    public ChatWebSocketHandler(ChatTicketService tickets) { this.tickets = tickets; }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.setTextMessageSizeLimit(2048);
        if (connections.size() >= 1000) { session.close(CloseStatus.SERVICE_OVERLOAD); return; }
        connections.put(session.getId(), new Connection(session));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Connection connection = connections.get(session.getId());
        if (connection == null) { session.close(CloseStatus.POLICY_VIOLATION); return; }
        try {
            var body = json.readTree(message.getPayload());
            if (connection.identity == null) {
                var identity = tickets.consume(body.path("ticket").asString(""));
                if (identity == null) { close(connection, CloseStatus.POLICY_VIOLATION); return; }
                connection.identity = identity;
                send(connection, Map.of("type", "ready"));
            } else if (!connection.identity.sessionExpiresAt().isAfter(Instant.now())) {
                close(connection, CloseStatus.POLICY_VIOLATION);
            } else if ("ping".equals(body.path("type").asString(""))) {
                send(connection, Map.of("type", "pong"));
            } else {
                // El socket solo recibe autenticación y heartbeat. El envío persistente usa REST.
                close(connection, CloseStatus.POLICY_VIOLATION);
            }
        } catch (RuntimeException exception) { close(connection, CloseStatus.BAD_DATA); }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void messageCommitted(MessageCreatedEvent event) {
        for (Connection connection : connections.values()) {
            var identity = connection.identity;
            if (identity != null && identity.sessionExpiresAt().isAfter(Instant.now())
                    && event.participantIds().contains(identity.userId())) {
                send(connection, Map.of("type", "message.created", "message", event.message()));
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void groupChanged(com.musa.chat.service.GroupChangedEvent event) {
        for (Connection connection : connections.values()) {
            var identity = connection.identity;
            if (identity != null && identity.sessionExpiresAt().isAfter(Instant.now())
                    && event.audience().contains(identity.userId())) {
                send(connection, Map.of("type", "conversation.updated", "conversationId", event.conversationId()));
            }
        }
    }

    private void send(Connection connection, Object body) {
        try { connection.session.sendMessage(new TextMessage(json.writeValueAsString(body))); }
        catch (IOException | RuntimeException exception) {
            log.debug("Se cerró una conexión de chat durante la entrega.");
            close(connection, CloseStatus.SERVER_ERROR);
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void expireConnections() {
        Instant now = Instant.now();
        for (Connection connection : connections.values()) {
            var identity = connection.identity;
            if ((identity == null && connection.opened.plusSeconds(5).isBefore(now))
                    || (identity != null && !identity.sessionExpiresAt().isAfter(now)))
                close(connection, CloseStatus.POLICY_VIOLATION);
        }
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { connections.remove(session.getId()); }
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Connection c = connections.remove(session.getId()); if (c != null) close(c, CloseStatus.SERVER_ERROR);
    }
    private void close(Connection connection, CloseStatus status) {
        connections.remove(connection.session.getId(), connection);
        try { connection.session.close(status); } catch (IOException ignored) { /* Conexión ya cerrada. */ }
    }
}
