package com.musa.social.config;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    public static UUID getUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
