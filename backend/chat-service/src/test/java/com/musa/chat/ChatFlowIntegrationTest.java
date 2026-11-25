package com.musa.chat;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "CHAT_TEST_ISOLATED", matches = "true")
class ChatFlowIntegrationTest {
    private static final Set<String> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static HttpServer usersServer;
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    @Value("${local.server.port}") int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        usersServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        usersServer.createContext("/api/v1/users/usernames", exchange -> {
            Map<String, String> result = new LinkedHashMap<>();
            for (String part : exchange.getRequestURI().getRawQuery().split("&")) {
                String id = URLDecoder.decode(part.split("=", 2)[1], StandardCharsets.UTF_8);
                if (ACTIVE.contains(id)) result.put(id, "test-user");
            }
            byte[] body = JSON.writeValueAsBytes(result);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) { out.write(body); }
        });
        usersServer.start();
        registry.add("spring.datasource.url", () -> System.getenv("CHAT_TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("CHAT_TEST_DB_USERNAME", "chat_test_owner"));
        registry.add("spring.datasource.password", () -> System.getenv("CHAT_TEST_DB_PASSWORD"));
        registry.add("musa.security.jwt.secret", () -> Base64.getEncoder().encodeToString(new byte[64]));
        registry.add("musa.services.users.base-url", () -> "http://127.0.0.1:" + usersServer.getAddress().getPort());
        registry.add("spring.config.import", () -> "");
    }
    @AfterAll static void closeUsers() { if (usersServer != null) usersServer.stop(0); }

    String user() { String id = UUID.randomUUID().toString(); ACTIVE.add(id); return id; }
    String token(String userId) throws Exception {
        var claims = new JWTClaimsSet.Builder().subject(userId).issuer("musa-auth").audience("musa-api")
                .expirationTime(Date.from(Instant.now().plusSeconds(600))).build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
        jwt.sign(new MACSigner(new byte[64])); return jwt.serialize();
    }
    HttpResponse<String> request(String method, String path, String user, Object body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/chat" + path))
                .timeout(Duration.ofSeconds(15)).header("Authorization", "Bearer " + token(user));
        if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
        else builder.header("Content-Type", "application/json").method(method,
                HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    JsonNode ok(HttpResponse<String> response) {
        assertEquals(200, response.statusCode(), response.body()); return JSON.readTree(response.body());
    }
    String create(String first, String second) throws Exception {
        return ok(request("POST", "/conversations/private", first, Map.of("recipientId", second))).path("id").asString();
    }
    @Test void simultaneousCreationProducesOneConversation() throws Exception {
        String first = user(), second = user();
        var executor = Executors.newFixedThreadPool(6);
        try {
            List<Future<String>> ids = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                boolean reverse = i % 2 == 0;
                ids.add(executor.submit(() -> create(reverse ? first : second, reverse ? second : first)));
            }
            Set<String> unique = new HashSet<>();
            for (var id : ids) unique.add(id.get(20, TimeUnit.SECONDS));
            assertEquals(1, unique.size());
            assertEquals(1, ok(request("GET", "/conversations", first, null)).path("items").size());
        } finally { executor.shutdownNow(); }
    }
    @Test void messagesPersistAndOutsiderCannotReadOrSend() throws Exception {
        String first = user(), second = user(), outsider = user(); String c = create(first, second);
        String path = "/conversations/" + c + "/messages";
        JsonNode sent = ok(request("POST", path, first, Map.of("content", "Hola MUSA")));
        assertEquals(first, sent.path("senderId").asString());
        JsonNode history = ok(request("GET", path, second, null));
        assertEquals(sent.path("id").asString(), history.path("items").get(0).path("id").asString());
        assertEquals(404, request("GET", path, outsider, null).statusCode());
        assertEquals(404, request("POST", path, outsider, Map.of("content", "intruso")).statusCode());
        assertEquals(0, ok(request("GET", "/conversations", outsider, null)).path("items").size());
    }
    @Test void rejectsBlankOversizedMissingRecipientAndInvalidPagination() throws Exception {
        String first = user(), second = user(); String path = "/conversations/" + create(first, second) + "/messages";
        assertEquals(400, request("POST", path, first, Map.of("content", "   ")).statusCode());
        assertEquals(400, request("POST", path, first, Map.of("content", "x".repeat(4001))).statusCode());
        assertEquals(400, request("GET", path + "?size=51", first, null).statusCode());
        assertEquals(400, request("GET", path + "?beforeId=" + UUID.randomUUID(), first, null).statusCode());
        assertEquals(400, request("POST", "/conversations/private", first, Map.of()).statusCode());
        assertEquals(404, request("POST", "/conversations/private", first,
                Map.of("recipientId", UUID.randomUUID().toString())).statusCode());
    }
    @Test void cursorDoesNotRepeatMessagesWhenNewMessagesArrive() throws Exception {
        String first = user(), second = user(); String path = "/conversations/" + create(first, second) + "/messages";
        Set<String> original = new HashSet<>();
        for (int i = 0; i < 35; i++) original.add(ok(request("POST", path, first, Map.of("content", "mensaje " + i))).path("id").asString());
        var initial = ok(request("GET", path, second, null));
        assertEquals(30, initial.path("items").size()); assertTrue(initial.path("hasMore").asBoolean());
        ok(request("POST", path, first, Map.of("content", "nuevo después de paginar")));
        String cursor = "?beforeCreatedAt=" + URLEncoder.encode(initial.path("nextCreatedAt").asString(), StandardCharsets.UTF_8)
                + "&beforeId=" + initial.path("nextId").asString();
        var older = ok(request("GET", path + cursor, second, null));
        assertEquals(5, older.path("items").size()); assertFalse(older.path("hasMore").asBoolean());
        Set<String> found = new HashSet<>();
        initial.path("items").forEach(m -> found.add(m.path("id").asString()));
        older.path("items").forEach(m -> assertTrue(found.add(m.path("id").asString())));
        assertEquals(original, found);
    }
    static class SocketListener implements WebSocket.Listener {
        final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        final CompletableFuture<Integer> closed = new CompletableFuture<>();
        final StringBuilder pending = new StringBuilder();
        @Override public void onOpen(WebSocket socket) { socket.request(1); }
        @Override public CompletionStage<?> onText(WebSocket socket, CharSequence text, boolean last) {
            pending.append(text); if (last) { frames.add(pending.toString()); pending.setLength(0); }
            socket.request(1); return null;
        }
        @Override public CompletionStage<?> onClose(WebSocket socket, int code, String reason) { closed.complete(code); return null; }
        @Override public void onError(WebSocket socket, Throwable error) { closed.completeExceptionally(error); }
    }
    WebSocket connect(String ticket, SocketListener listener) throws Exception {
        var socket = HTTP.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5))
                .buildAsync(URI.create("ws://127.0.0.1:" + port + "/api/v1/chat/ws"), listener).get(5, TimeUnit.SECONDS);
        socket.sendText(JSON.writeValueAsString(Map.of("ticket", ticket)), true).join(); return socket;
    }
    String ticket(String user) throws Exception { return ok(request("POST", "/realtime/tickets", user, null)).path("ticket").asString(); }
    @Test void webSocketDeliversCommittedMessageOnlyToParticipantsAndRejectsReplayedTicket() throws Exception {
        String first = user(), second = user(), outsider = user(); String c = create(first, second);
        String singleUse = ticket(second);
        var a = new SocketListener(); var b = new SocketListener(); var third = new SocketListener();
        WebSocket socketA = connect(ticket(first), a), socketB = connect(singleUse, b), socketThird = connect(ticket(outsider), third);
        try {
            assertEquals("ready", JSON.readTree(a.frames.poll(5, TimeUnit.SECONDS)).path("type").asString());
            assertEquals("ready", JSON.readTree(b.frames.poll(5, TimeUnit.SECONDS)).path("type").asString());
            assertEquals("ready", JSON.readTree(third.frames.poll(5, TimeUnit.SECONDS)).path("type").asString());
            var replay = new SocketListener(); var replaySocket = connect(singleUse, replay);
            assertEquals(1008, replay.closed.get(5, TimeUnit.SECONDS)); replaySocket.abort();
            var sent = ok(request("POST", "/conversations/" + c + "/messages", first, Map.of("content", "Tiempo real")));
            String firstFrame = a.frames.poll(5, TimeUnit.SECONDS), secondFrame = b.frames.poll(5, TimeUnit.SECONDS);
            assertEquals(sent.path("id").asString(), JSON.readTree(firstFrame).path("message").path("id").asString());
            assertEquals(sent.path("id").asString(), JSON.readTree(secondFrame).path("message").path("id").asString());
            assertNull(third.frames.poll(250, TimeUnit.MILLISECONDS));
            assertEquals(400, request("POST", "/conversations/" + c + "/messages", first, Map.of("content", " ")).statusCode());
            assertNull(a.frames.poll(250, TimeUnit.MILLISECONDS));
        } finally { socketA.abort(); socketB.abort(); socketThird.abort(); }
    }
}
