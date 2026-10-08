package com.musa.users.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Datos del usuario y tokens de su sesión. */
public record AuthResponseDto(
        String accessToken,
        String refreshToken,
        UUID userId,
        String fullName,
        String roleName,
        Instant accessTokenExpiresAt,
        Instant sessionExpiresAt,
        String username
) {}