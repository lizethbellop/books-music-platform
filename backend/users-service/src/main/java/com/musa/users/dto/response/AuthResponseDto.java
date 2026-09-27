package com.musa.users.dto.response;

import java.util.UUID;

public record AuthResponseDto(
        String accessToken,
        String refreshToken,
        UUID userId,
        String fullName,
        String roleName
) {}