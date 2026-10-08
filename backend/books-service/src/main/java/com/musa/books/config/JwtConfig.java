package com.musa.books.config;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${musa.security.jwt.secret}") String secret
    ) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);

        if (keyBytes.length < 64) {
            throw new IllegalArgumentException(
                    "HS512 requiere una clave JWT de al menos 64 bytes."
            );
        }

        var key = new SecretKeySpec(keyBytes, "HmacSHA512");

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();

        var audienceValidator = new JwtClaimValidator<List<String>>(
                "aud",
                audience -> audience != null
                        && audience.contains("musa-api")
        );

        var subjectValidator = new JwtClaimValidator<String>(
                "sub",
                JwtConfig::isUuid
        );

        var expirationRequired = new JwtClaimValidator<Instant>(
                "exp",
                Objects::nonNull
        );

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        new JwtTimestampValidator(Duration.ZERO),
                        new JwtIssuerValidator("musa-auth"),
                        audienceValidator,
                        subjectValidator,
                        expirationRequired
                )
        );

        return decoder;
    }

    private static boolean isUuid(String value) {
        if (value == null) {
            return false;
        }

        try {
            return UUID.fromString(value).toString()
                    .equalsIgnoreCase(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}