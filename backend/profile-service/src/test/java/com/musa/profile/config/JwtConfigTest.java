package com.musa.profile.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.junit.jupiter.api.Assertions.*;

class JwtConfigTest {

    private byte[] key;
    private JwtDecoder decoder;
    private UUID userId;

    @BeforeEach
    void setUp() {
        key = new byte[64];
        new SecureRandom().nextBytes(key);

        decoder = new JwtConfig().jwtDecoder(
                Base64.getEncoder().encodeToString(key)
        );

        userId = UUID.randomUUID();
    }

    private String token(
            String subject,
            String issuer,
            String audience,
            Instant expiresAt,
            byte[] signingKey
    ) throws JOSEException {
        var claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(Instant.now()));

        if (expiresAt != null) {
            claims.expirationTime(Date.from(expiresAt));
        }

        var jwt = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS512),
                claims.build()
        );

        jwt.sign(new MACSigner(signingKey));
        return jwt.serialize();
    }

    @Test
    void acceptsValidTokenAndReadsUuid() throws Exception {
        String token = token(
                userId.toString(), "musa-auth", "musa-api",
                Instant.now().plusSeconds(900), key
        );

        assertEquals(userId.toString(), decoder.decode(token).getSubject());
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String token = token(
                userId.toString(), "musa-auth", "musa-api",
                Instant.now().minusSeconds(60), key
        );

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void rejectsWrongIssuer() throws Exception {
        String token = token(
                userId.toString(), "otro-emisor", "musa-api",
                Instant.now().plusSeconds(900), key
        );

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void rejectsWrongAudience() throws Exception {
        String token = token(
                userId.toString(), "musa-auth", "otra-api",
                Instant.now().plusSeconds(900), key
        );

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void rejectsSubjectThatIsNotUuid() throws Exception {
        String token = token(
                "test@example.invalid", "musa-auth", "musa-api",
                Instant.now().plusSeconds(900), key
        );

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void rejectsWrongSignature() throws Exception {
        byte[] otherKey = new byte[64];
        new SecureRandom().nextBytes(otherKey);

        String token = token(
                userId.toString(), "musa-auth", "musa-api",
                Instant.now().plusSeconds(900), otherKey
        );

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void rejectsTokenWithoutExpiration() throws Exception {
        String token = token(
                userId.toString(), "musa-auth", "musa-api",
                null, key
        );

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }
}