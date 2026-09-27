package com.musa.users.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenRequestDto(
        @NotBlank(message = "El token de refresco es obligatorio")
        String refreshToken
) {}
