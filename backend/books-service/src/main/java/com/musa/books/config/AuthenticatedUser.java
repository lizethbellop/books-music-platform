package com.musa.books.config;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

public final class AuthenticatedUser {
    private AuthenticatedUser() {}

    public static UUID requireOwn(Jwt jwt, UUID suppliedUserId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        if (suppliedUserId != null && !userId.equals(suppliedUserId)) {
            throw new AccessDeniedException("Solo puedes acceder a tus propios datos.");
        }
        return userId;
    }
}
