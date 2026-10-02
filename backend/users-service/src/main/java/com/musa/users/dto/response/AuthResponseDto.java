package com.musa.users.dto.response;

import java.util.UUID;

/** DTO de respuesta que contiene la información del usuario autenticado y los tokens JWT generados. */
public record AuthResponseDto(
        String accessToken,
        String refreshToken,
        UUID userId,
        String fullName,
        String roleName
) {}