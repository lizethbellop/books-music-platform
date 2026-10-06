package com.musa.users.dto.request;

import jakarta.validation.constraints.NotBlank;

/** DTO de solicitud para validar la existencia y vigencia de un token de recuperación. */
public record VerifyTokenRequestDto(
        @NotBlank(message = "El token de recuperación es obligatorio")
        String token
) {}
