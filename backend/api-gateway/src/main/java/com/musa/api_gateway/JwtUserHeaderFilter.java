package com.musa.api_gateway;

import java.time.Duration;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtUserHeaderFilter implements GlobalFilter, Ordered {

    private final NimbusJwtDecoder jwtDecoder;

    public JwtUserHeaderFilter(@Value("${musa.security.jwt.secret}") String secret) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        SecretKeySpec key = new SecretKeySpec(keyBytes, "HmacSHA512");

        this.jwtDecoder = NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();

        this.jwtDecoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        new JwtTimestampValidator(Duration.ZERO),
                        new JwtIssuerValidator("musa-auth")
                )
        );
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Rutas públicas (ej. /api/v1/auth/login): limpia cualquier X-User-Id falso y deja pasar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ServerHttpRequest sanitized = exchange.getRequest().mutate()
                    .headers(headers -> headers.remove("X-User-Id"))
                    .build();
            return chain.filter(exchange.mutate().request(sanitized).build());
        }

        String token = authHeader.substring(7);
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String userId = jwt.getSubject();

            // Reemplaza X-User-Id con el UUID verificado y conserva Authorization para books/music
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove("X-User-Id");
                        if (userId != null && !userId.isBlank()) {
                            headers.set("X-User-Id", userId);
                        }
                    })
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}