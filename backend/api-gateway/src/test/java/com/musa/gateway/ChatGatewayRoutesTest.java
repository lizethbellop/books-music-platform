package com.musa.gateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(classes = com.musa.api_gateway.ApiGatewayApplication.class)
class ChatGatewayRoutesTest {
    @Autowired RouteDefinitionLocator routes;
    @Autowired org.springframework.cloud.gateway.config.GlobalCorsProperties cors;
    @Test void chatWebSocketRoutePrecedesRestAndUsersDirectoryIsRouted() {
        var definitions = routes.getRouteDefinitions().collectList().block(Duration.ofSeconds(5));
        assertNotNull(definitions);
        var chatCors = cors.getCorsConfigurations().get("/api/v1/chat/**");
        assertNotNull(chatCors);
        assertTrue(chatCors.getAllowedMethods().containsAll(java.util.List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS")));
        var notifications = definitions.stream().filter(route -> route.getId().equals("notification-service")).findFirst().orElseThrow();
        assertEquals(8090, notifications.getUri().getPort());
        assertTrue(notifications.getPredicates().toString().contains("/api/v1/notifications/**"));
        var ws = definitions.stream().filter(r -> r.getId().equals("chat-websocket")).findFirst().orElseThrow();
        var rest = definitions.stream().filter(r -> r.getId().equals("chat-service")).findFirst().orElseThrow();
        var users = definitions.stream().filter(r -> r.getId().equals("users-directory")).findFirst().orElseThrow();
        assertEquals("ws", ws.getUri().getScheme());
        assertTrue(definitions.indexOf(ws) < definitions.indexOf(rest));
        assertTrue(ws.getPredicates().toString().contains("/api/v1/chat/ws"));
        assertTrue(users.getPredicates().toString().contains("/api/v1/users/**"));
    }
}
