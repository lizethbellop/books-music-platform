package com.musa.notifications;

import com.musa.notifications.config.RabbitMQConfig;
import com.musa.notifications.dto.NotificationEvent;
import com.musa.notifications.entity.NotificationType;
import com.musa.notifications.service.NotificationService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.dao.TransientDataAccessResourceException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "NOTIFICATIONS_TEST_ISOLATED", matches = "true")
class NotificationFlowIntegrationTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @Autowired JdbcTemplate jdbc;
    @Autowired RabbitTemplate rabbit;
    @MockitoSpyBean NotificationService service;
    @Value("${local.server.port}") int port;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("NOTIFICATIONS_TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> "integration_owner");
        registry.add("spring.datasource.password", () -> System.getenv("NOTIFICATIONS_TEST_DB_PASSWORD"));
        registry.add("spring.rabbitmq.host", () -> "127.0.0.1");
        registry.add("spring.rabbitmq.port", () -> System.getenv("NOTIFICATIONS_TEST_RABBIT_PORT"));
        registry.add("spring.rabbitmq.username", () -> "integration");
        registry.add("spring.rabbitmq.password", () -> System.getenv("NOTIFICATIONS_TEST_RABBIT_PASSWORD"));
        registry.add("musa.security.jwt.secret", () -> Base64.getEncoder().encodeToString(new byte[64]));
        registry.add("spring.config.import", () -> "");
    }
    private NotificationEvent event(NotificationType type, UUID recipient) {
        UUID actor = UUID.randomUUID();
        return new NotificationEvent(1, UUID.randomUUID(), type, recipient, actor,
                type == NotificationType.USER_FOLLOWED ? actor : UUID.randomUUID());
    }
    private String routing(NotificationType type) {
        return switch(type) {
            case CHAT_MESSAGE_CREATED -> "chat.message.created";
            case USER_FOLLOWED -> "social.user.followed";
            case POST_LIKED -> "social.post.liked";
            case POST_COMMENTED -> "social.post.commented";
            case GROUP_MEMBER_ADDED -> "chat.group.member.added";
            case GROUP_MEMBER_REMOVED -> "chat.group.member.removed";
            case GROUP_ADMIN_CHANGED -> "chat.group.admin.changed";
            case POST_PUBLISHED -> "social.post.published";
        };
    }
    private void publish(NotificationEvent event) {
        MessageProperties p = new MessageProperties(); p.setContentType("application/json");
        p.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        rabbit.send(RabbitMQConfig.EVENTS_EXCHANGE, routing(event.type()), new Message(JSON.writeValueAsBytes(event), p));
    }
    private int count(UUID recipient) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE recipient_id = ?", Integer.class, recipient);
    }
    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(12).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(50);
        assertTrue(condition.getAsBoolean(), "No se alcanzó el resultado esperado.");
    }
    private String token(UUID user) throws Exception {
        var claims = new JWTClaimsSet.Builder().subject(user.toString()).issuer("musa-auth").audience("musa-api")
                .expirationTime(Date.from(Instant.now().plusSeconds(300))).build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
        jwt.sign(new MACSigner(new byte[64])); return jwt.serialize();
    }
    private HttpResponse<String> request(String method, String path, UUID user) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/notifications" + path))
                .timeout(Duration.ofSeconds(10)).method(method, HttpRequest.BodyPublishers.noBody());
        if (user != null) b.header("Authorization", "Bearer " + token(user));
        return HttpClient.newHttpClient().send(b.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void consumesAllSupportedTypesAndDeduplicates() throws Exception {
        UUID recipient = UUID.randomUUID();
        for (var type : NotificationType.values()) { var event = event(type, recipient); publish(event); publish(event); }
        await(() -> count(recipient) == 8);
        var page = request("GET", "", recipient); assertEquals(200, page.statusCode());
        assertEquals(8, JSON.readTree(page.body()).path("items").size());
        assertEquals(8, JSON.readTree(request("GET", "/unread-count", recipient).body()).path("count").asInt());
    }
    @Test void ownershipReadAndReadAllAreEnforced() throws Exception {
        UUID recipient = UUID.randomUUID(), outsider = UUID.randomUUID(); publish(event(NotificationType.POST_LIKED, recipient));
        await(() -> count(recipient) == 1);
        var items = JSON.readTree(request("GET", "", recipient).body()).path("items");
        String id = items.get(0).path("id").asString();
        assertEquals(0, JSON.readTree(request("GET", "", outsider).body()).path("items").size());
        assertEquals(404, request("POST", "/" + id + "/read", outsider).statusCode());
        assertEquals(204, request("POST", "/" + id + "/read", recipient).statusCode());
        String readAt = JSON.readTree(request("GET", "", recipient).body()).path("items").get(0).path("readAt").asString();
        assertEquals(204, request("POST", "/" + id + "/read", recipient).statusCode());
        assertEquals(readAt, JSON.readTree(request("GET", "", recipient).body()).path("items").get(0).path("readAt").asString());
        publish(event(NotificationType.POST_COMMENTED, recipient)); await(() -> count(recipient) == 2);
        assertEquals(200, request("POST", "/read-all", outsider).statusCode());
        assertEquals(1, JSON.readTree(request("GET", "/unread-count", recipient).body()).path("count").asInt());
        assertEquals(200, request("POST", "/read-all", recipient).statusCode());
        assertEquals(0, JSON.readTree(request("GET", "?unreadOnly=true", recipient).body()).path("items").size());
    }
    @Test void requiresAuthenticationAndValidPagination() throws Exception {
        assertEquals(401, request("GET", "", null).statusCode());
        assertEquals(400, request("GET", "?size=51", UUID.randomUUID()).statusCode());
        assertEquals(400, request("GET", "?page=-1", UUID.randomUUID()).statusCode());
        assertEquals(400, request("POST", "/invalid-uuid/read", UUID.randomUUID()).statusCode());
    }
    @Test void temporaryFailureRetriesAndStoresOneNotification() throws Exception {
        UUID recipient = UUID.randomUUID(); var event = event(NotificationType.POST_LIKED, recipient);
        doThrow(new TransientDataAccessResourceException("simulated temporary failure"))
                .doCallRealMethod().when(service).process(event);
        publish(event); await(() -> count(recipient) == 1);
        verify(service, times(2)).process(event);
    }
    @Test void invalidJsonGoesToDlq() {
        String body = "invalid-json-" + UUID.randomUUID();
        rabbit.send(RabbitMQConfig.EVENTS_EXCHANGE, "chat.message.created", new Message(body.getBytes(), new MessageProperties()));
        var failed = rabbit.receive(RabbitMQConfig.DEAD_LETTER_QUEUE, 10000);
        assertNotNull(failed); assertEquals(body, new String(failed.getBody()));
    }
    @Test void mismatchedRoutingGoesToDlqAndIsNotStored() {
        UUID recipient = UUID.randomUUID(); var event = event(NotificationType.POST_LIKED, recipient);
        rabbit.send(RabbitMQConfig.EVENTS_EXCHANGE, "social.user.followed", new Message(JSON.writeValueAsBytes(event), new MessageProperties()));
        var failed = rabbit.receive(RabbitMQConfig.DEAD_LETTER_QUEUE, 10000);
        assertNotNull(failed); assertEquals(event.eventId().toString(), JSON.readTree(failed.getBody()).path("eventId").asString());
        assertEquals(0, count(recipient));
    }
}
