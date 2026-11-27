package com.musa.api_gateway;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecommendationRouteTest {
    static final HttpServer upstream;
    static final AtomicReference<String> forwarded = new AtomicReference<>();
    static {
        try {
            upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            upstream.createContext("/api/recommendations/books/me", exchange -> {
                String bearer = exchange.getRequestHeaders().getFirst("Authorization");
                forwarded.set(exchange.getRequestURI().getPath()+"|"+bearer);
                byte[] body = (bearer == null ? "{\"detail\":\"Unauthorized\"}"
                        : "{\"items\":[{\"externalId\":\"OL1W\"}]}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(bearer == null ? 401 : 200, body.length);
                try (var stream = exchange.getResponseBody()) { stream.write(body); }
            });
            upstream.start();
        } catch (Exception error) { throw new ExceptionInInitializerError(error); }
    }

    @DynamicPropertySource static void upstreamUrl(DynamicPropertyRegistry registry) {
        registry.add("RECOMMENDATIONS_SERVICE_URL",
                () -> "http://127.0.0.1:"+upstream.getAddress().getPort());
    }

    @Value("${local.server.port}") int gatewayPort;
    @AfterAll static void stop() { upstream.stop(0); }

    @Test void homePathAndBearerReachRecommendationsService() {
        String response = RestClient.create("http://127.0.0.1:"+gatewayPort).get()
                .uri("/api/recommendations/books/me")
                .header("Authorization", "Bearer session-token")
                .retrieve().body(String.class);
        assertTrue(response.contains("OL1W"));
        assertEquals("/api/recommendations/books/me|Bearer session-token", forwarded.get());
    }

    @Test void acceptsFlutterWebPreflightWithoutAToken() {
        var response = RestClient.create("http://127.0.0.1:"+gatewayPort)
                .method(org.springframework.http.HttpMethod.OPTIONS)
                .uri("/api/recommendations/books/me")
                .header("Origin", "http://localhost:5555")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization")
                .retrieve().toBodilessEntity();
        assertEquals(200, response.getStatusCode().value());
        assertEquals("http://localhost:5555", response.getHeaders().getFirst("Access-Control-Allow-Origin"));
        assertTrue(response.getHeaders().getFirst("Access-Control-Allow-Headers").toLowerCase().contains("authorization"));
    }

    @Test void rejectsAnUnconfiguredWebOrigin() {
        var error = assertThrows(HttpClientErrorException.class, () ->
                RestClient.create("http://127.0.0.1:"+gatewayPort)
                        .method(org.springframework.http.HttpMethod.OPTIONS)
                        .uri("/api/recommendations/books/me")
                        .header("Origin", "https://unconfigured.example")
                        .header("Access-Control-Request-Method", "GET")
                        .retrieve().toBodilessEntity());
        assertEquals(403, error.getStatusCode().value());
    }

    @Test void preservesServiceUnauthorizedResponse() {
        var error = assertThrows(HttpClientErrorException.class, () ->
                RestClient.create("http://127.0.0.1:"+gatewayPort).get()
                        .uri("/api/recommendations/books/me").retrieve().body(String.class));
        assertEquals(401, error.getStatusCode().value());
    }
}
