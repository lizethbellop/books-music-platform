package com.musa.communities_service.polls.realtime;

import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class PollWebSocketHandler extends TextWebSocketHandler {

    private final PollTicketService tickets;

    private final JsonMapper json = JsonMapper.builder().build();

    private final Map<String, Connection> connections =
            new ConcurrentHashMap<>();

    private final ThreadPoolExecutor sender = new ThreadPoolExecutor(
            2,
            4,
            30,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(128),
            new ThreadPoolExecutor.AbortPolicy()
    );

    private static final class Connection {

        final WebSocketSession session;
        final Instant opened = Instant.now();

        volatile Instant lastSeen = Instant.now();
        volatile PollTicketService.Identity identity;

        final AtomicBoolean dirty = new AtomicBoolean();
        final AtomicBoolean pong = new AtomicBoolean();
        final AtomicBoolean sending = new AtomicBoolean();
        final AtomicBoolean ready = new AtomicBoolean();

        Connection(WebSocketSession session) {
            this.session = new ConcurrentWebSocketSessionDecorator(
                    session,
                    2000,
                    8192
            );
        }
    }

    public PollWebSocketHandler(PollTicketService tickets) {
        this.tickets = tickets;
    }

    @Override
    public synchronized void afterConnectionEstablished(
            WebSocketSession session
    ) throws Exception {
        session.setTextMessageSizeLimit(1024);
        session.setBinaryMessageSizeLimit(0);

        if (connections.size() >= 200) {
            session.close(CloseStatus.SERVICE_OVERLOAD);
            return;
        }

        connections.put(
                session.getId(),
                new Connection(session)
        );
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) {
        Connection connection = connections.get(session.getId());

        if (connection == null) {
            return;
        }

        try {
            var body = json.readTree(message.getPayload());

            if (connection.identity == null) {
                var identity = tickets.consume(
                        body.path("ticket").asString("")
                );

                if (identity == null) {
                    close(connection, CloseStatus.POLICY_VIOLATION);
                    return;
                }

                synchronized (this) {
                    long userConnections = connections.values().stream()
                            .filter(other -> other.identity != null
                                    && other.identity.userId()
                                    .equals(identity.userId()))
                            .count();

                    if (userConnections >= 3) {
                        close(connection, CloseStatus.SERVICE_OVERLOAD);
                        return;
                    }

                    connection.identity = identity;
                }

                connection.ready.set(true);
                connection.dirty.set(true);

            } else if ("ping".equals(
                    body.path("type").asString("")
            )) {
                connection.lastSeen = Instant.now();
                connection.pong.set(true);

            } else {
                close(connection, CloseStatus.POLICY_VIOLATION);
            }

        } catch (RuntimeException exception) {
            close(connection, CloseStatus.BAD_DATA);
        }
    }

    public void invalidate(UUID pollId) {
        for (Connection connection : connections.values()) {
            var identity = connection.identity;

            if (identity != null
                    && identity.pollId().equals(pollId)) {
                connection.dirty.set(true);
            }
        }
    }

    @Scheduled(
            fixedDelay = 500,
            scheduler = "pollMaintenanceScheduler"
    )
    public void flush() {
        Instant now = Instant.now();

        for (Connection connection : connections.values()) {
            var identity = connection.identity;

            boolean authenticationExpired =
                    identity == null
                            && connection.opened.plusSeconds(5)
                            .isBefore(now);

            boolean sessionExpired =
                    identity != null
                            && !identity.expiresAt().isAfter(now);

            boolean inactive =
                    connection.lastSeen.plusSeconds(60).isBefore(now);

            if (authenticationExpired || sessionExpired || inactive) {
                close(connection, CloseStatus.POLICY_VIOLATION);
                continue;
            }

            boolean pending = connection.dirty.get()
                    || connection.pong.get()
                    || connection.ready.get();

            if (identity == null || !pending) {
                continue;
            }

            if (!connection.sending.compareAndSet(false, true)) {
                continue;
            }

            try {
                sender.execute(() -> {
                    try {
                        if (connection.ready.getAndSet(false)) {
                            send(connection, "ready");
                        }

                        if (connection.pong.getAndSet(false)) {
                            send(connection, "pong");
                        }

                        if (connection.dirty.getAndSet(false)) {
                            send(connection, "poll.updated");
                        }
                    } finally {
                        connection.sending.set(false);
                    }
                });

            } catch (RejectedExecutionException exception) {
                connection.sending.set(false);
                close(connection, CloseStatus.SERVICE_OVERLOAD);
            }
        }
    }

    private void send(Connection connection, String type) {
        if (!connection.identity.expiresAt().isAfter(Instant.now())) {
            close(connection, CloseStatus.POLICY_VIOLATION);
            return;
        }

        try {
            String payload = json.writeValueAsString(Map.of(
                    "type", type,
                    "pollId", connection.identity.pollId()
            ));

            connection.session.sendMessage(
                    new TextMessage(payload)
            );

        } catch (IOException | RuntimeException exception) {
            close(connection, CloseStatus.SERVER_ERROR);
        }
    }

    private void close(
            Connection connection,
            CloseStatus status
    ) {
        connections.remove(
                connection.session.getId(),
                connection
        );

        try {
            connection.session.close(status);
        } catch (IOException ignored) {
            // La conexión puede haberse cerrado previamente.
        }
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {
        connections.remove(session.getId());
    }

    @Override
    public void handleTransportError(
            WebSocketSession session,
            Throwable exception
    ) {
        Connection connection = connections.get(session.getId());

        if (connection != null) {
            close(connection, CloseStatus.SERVER_ERROR);
        }
    }

    @PreDestroy
    public void shutdown() {
        sender.shutdownNow();

        for (Connection connection : connections.values()) {
            close(connection, CloseStatus.GOING_AWAY);
        }
    }
}