package com.musa.users.service.impl;
import com.musa.users.service.JwtService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.time.Instant;
import java.util.UUID;

/**
 * Implementación del servicio de utilidades JWT para la firma, desencriptación y validación de tokens.
 */
@Service
public class JwtServiceImpl implements JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(new HashMap<>(), userDetails);
    }

    @Override
    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, claims -> {
            String email = claims.get("email", String.class);

            return email != null ? email : claims.getSubject();
        });
    }

    @Override
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public String generateAccessToken(
            UserDetails userDetails,
            Instant sessionExpiresAt
    ) {
        Instant now = Instant.now();

        if (!sessionExpiresAt.isAfter(now)) {
            throw new IllegalArgumentException("La sesión ya venció.");
        }

        Instant accessExpiresAt = now.plusMillis(jwtExpiration);

        if (accessExpiresAt.isAfter(sessionExpiresAt)) {
            accessExpiresAt = sessionExpiresAt;
        }

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(accessExpiresAt))
                .signWith(getSigningKey())
                .compact();
    }

    @Override
    public Instant getAccessTokenExpiresAt(String token) {
        return extractExpiration(token).toInstant();
    }

    @Override
    public String generateAccessToken(
            UserDetails userDetails,
            UUID userId,
            Instant sessionExpiresAt
    ) {
        Instant now = Instant.now();

        if (!sessionExpiresAt.isAfter(now)) {
            throw new IllegalArgumentException("La sesión ya venció.");
        }

        Instant accessExpiresAt = now.plusMillis(jwtExpiration);

        if (accessExpiresAt.isAfter(sessionExpiresAt)) {
            accessExpiresAt = sessionExpiresAt;
        }

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", userDetails.getUsername())
                .issuer("musa-auth")
                .audience().add("musa-api").and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(accessExpiresAt))
                .signWith(getSigningKey())
                .compact();
    }
}





