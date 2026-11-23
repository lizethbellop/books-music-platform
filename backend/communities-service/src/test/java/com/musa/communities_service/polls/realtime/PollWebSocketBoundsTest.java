package com.musa.communities_service.polls.realtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.web.socket.*;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("concurrency")
@Timeout(10)
class PollWebSocketBoundsTest {

    private WebSocketSession session(String id) {
        var session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private PollTicketService.Identity identity(UUID user) {
        return new PollTicketService.Identity(user, UUID.randomUUID(), UUID.randomUUID(),
                Instant.now().plusSeconds(60), Instant.now().plusSeconds(30));
    }

    @Test
    @DisplayName("Rechaza la conexión 201 y mantiene acotado el registro de sockets")
    void rejectsConnectionsBeyondGlobalLimit() throws Exception {
        var handler = new PollWebSocketHandler(mock(PollTicketService.class));
        try {
            for (int i = 0; i < 200; i++) handler.afterConnectionEstablished(session("s" + i));
            var extra = session("extra");
            handler.afterConnectionEstablished(extra);
            verify(extra).close(CloseStatus.SERVICE_OVERLOAD);
        } finally {
            handler.shutdown();
        }
    }

    @Test
    @DisplayName("Un usuario no puede mantener cuatro sockets autenticados")
    void rejectsFourthAuthenticatedConnectionForSameUser() throws Exception {
        var tickets = mock(PollTicketService.class);
        when(tickets.consume(anyString())).thenReturn(identity(UUID.randomUUID()));
        var handler = new PollWebSocketHandler(tickets);
        try {
            for (int i = 0; i < 3; i++) authenticate(handler, session("s" + i));
            var fourth = session("fourth");
            authenticate(handler, fourth);
            verify(fourth).close(CloseStatus.SERVICE_OVERLOAD);
        } finally {
            handler.shutdown();
        }
    }

    @Test
    @DisplayName("Clientes lentos activan rechazo y nunca superan cuatro envíos simultáneos")
    void slowConsumersCannotCreateUnboundedThreadsOrQueue() throws Exception {
        var tickets = mock(PollTicketService.class);
        when(tickets.consume(anyString())).thenAnswer(call -> identity(UUID.randomUUID()));
        var handler = new PollWebSocketHandler(tickets);
        var release = new CountDownLatch(1);
        var fourSendersStarted = new CountDownLatch(4);
        var completed = new CountDownLatch(132);
        var active = new AtomicInteger();
        var peak = new AtomicInteger();
        var rejected = new AtomicInteger();
        var overlappingSameSession = new AtomicInteger();
        try {
            for (int i = 0; i < 200; i++) {
                var session = session("slow" + i);
                var sessionActive = new AtomicInteger();
                doAnswer(call -> {
                    if (sessionActive.incrementAndGet() > 1) overlappingSameSession.incrementAndGet();
                    peak.accumulateAndGet(active.incrementAndGet(), Math::max);
                    fourSendersStarted.countDown();
                    try {
                        release.await();
                    } finally {
                        active.decrementAndGet();
                        sessionActive.decrementAndGet();
                        if (((TextMessage) call.getArgument(0)).getPayload().contains("poll.updated")) completed.countDown();
                    }
                    return null;
                }).when(session).sendMessage(any());
                doAnswer(call -> {
                    if (CloseStatus.SERVICE_OVERLOAD.equals(call.getArgument(0))) rejected.incrementAndGet();
                    return null;
                }).when(session).close(any());
                authenticate(handler, session);
            }
            handler.flush();
            assertTrue(fourSendersStarted.await(2, TimeUnit.SECONDS), "Los cuatro trabajadores deben ejecutar envíos reales");
            for (int i = 0; i < 5; i++) handler.flush();
            assertEquals(4, peak.get());
            assertEquals(0, overlappingSameSession.get(), "No se debe enviar concurrentemente sobre el mismo socket");
            assertTrue(rejected.get() >= 68, "200 conexiones superan los 4 trabajadores y las 128 tareas en cola");

            release.countDown();
            assertTrue(completed.await(3, TimeUnit.SECONDS), "La cola debe drenarse al recuperarse los clientes");
            var healthy = session("healthy-after-overload");
            authenticate(handler, healthy);
            handler.flush();
            verify(healthy, timeout(2000).atLeastOnce()).sendMessage(any());
        } finally {
            release.countDown();
            handler.shutdown();
        }
    }

    @Test
    @DisplayName("Apagar el servicio interrumpe envíos bloqueados y cierra sus conexiones")
    void shutdownInterruptsBlockedSendersAndClosesConnections() throws Exception {
        var tickets = mock(PollTicketService.class);
        when(tickets.consume(anyString())).thenAnswer(call -> identity(UUID.randomUUID()));
        var handler = new PollWebSocketHandler(tickets);
        var started = new CountDownLatch(2);
        var interrupted = new CountDownLatch(2);
        var session1 = session("shutdown-1");
        var session2 = session("shutdown-2");
        try {
            for (var session : new WebSocketSession[]{session1, session2}) {
                doAnswer(call -> {
                    started.countDown();
                    try {
                        new CountDownLatch(1).await();
                    } catch (InterruptedException exception) {
                        interrupted.countDown();
                        Thread.currentThread().interrupt();
                        throw new java.io.IOException("Simulación de envío interrumpido", exception);
                    }
                    return null;
                }).when(session).sendMessage(any());
                authenticate(handler, session);
            }
            handler.flush();
            assertTrue(started.await(2, TimeUnit.SECONDS));
            handler.shutdown();
            assertTrue(interrupted.await(2, TimeUnit.SECONDS), "Los envíos no deben quedar bloqueados después del cierre");
            verify(session1, timeout(1000).atLeastOnce()).close(any());
            verify(session2, timeout(1000).atLeastOnce()).close(any());
        } finally {
            handler.shutdown();
        }
    }

    @Test
    @DisplayName("Un ticket inválido no habilita el socket")
    void invalidTicketCannotAuthenticate() throws Exception {
        var handler = new PollWebSocketHandler(mock(PollTicketService.class));
        try {
            var session = session("invalid");
            authenticate(handler, session);
            verify(session).close(CloseStatus.POLICY_VIOLATION);
        } finally {
            handler.shutdown();
        }
    }

    private void authenticate(PollWebSocketHandler handler, WebSocketSession session) throws Exception {
        handler.afterConnectionEstablished(session);
        handler.handleMessage(session, new TextMessage("{\"ticket\":\"valid\"}"));
    }
}
