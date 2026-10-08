package com.musa.users.service.impl;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;

class JwtSessionExpiryTest {
    private JwtServiceImpl service;
    private final UserDetails user = User.withUsername("test@example.invalid")
            .password("").roles("USER").build();

    @BeforeEach
    void setup() {
        byte[] key = new byte[64];
        new SecureRandom().nextBytes(key);
        service = new JwtServiceImpl();
        ReflectionTestUtils.setField(service, "secretKey", Base64.getEncoder().encodeToString(key));
        ReflectionTestUtils.setField(service, "jwtExpiration", 900000L);
    }

    @Test
    void accessLastsFifteenMinutes() {
        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String token = service.generateAccessToken(user, Instant.now().plusSeconds(604800));
        Instant after = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiry = service.getAccessTokenExpiresAt(token);
        assertFalse(expiry.isBefore(before.plusSeconds(900)));
        assertFalse(expiry.isAfter(after.plusSeconds(900)));
        assertTrue(service.isTokenValid(token, user));
    }

    @Test
    void accessCannotOutliveSession() {
        Instant end = Instant.now().plusSeconds(120).truncatedTo(ChronoUnit.SECONDS);
        String token = service.generateAccessToken(user, end);
        assertEquals(end, service.getAccessTokenExpiresAt(token));
    }

    @Test
    void rejectsExpiredSession() {
        assertThrows(IllegalArgumentException.class,
                () -> service.generateAccessToken(user, Instant.now().minusSeconds(1)));
    }

    @Test
    void tokenContainsUuidAndRemainsValidForAuthentication() {
        UUID userId = UUID.randomUUID();

        String token = service.generateAccessToken(
                user,
                userId,
                Instant.now().plusSeconds(604800)
        );

        var claims = service.extractClaim(token, value -> value);

        assertAll(
                () -> assertEquals(userId.toString(), claims.getSubject()),
                () -> assertEquals("musa-auth", claims.getIssuer()),
                () -> assertTrue(claims.getAudience().contains("musa-api")),
                () -> assertEquals(user.getUsername(), service.extractUsername(token)),
                () -> assertTrue(service.isTokenValid(token, user))
        );
    }
}
