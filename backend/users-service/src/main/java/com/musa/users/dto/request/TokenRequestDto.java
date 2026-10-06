package com.musa.users.dto.request;

import jakarta.validation.constraints.NotBlank;

/** DTO de solicitud para refrescar token o cerrar sesión utilizando un token de refresco. */
public record TokenRequestDto(
        @NotBlank(message = "El token de refresco es obligatorio")
        String refreshToken
) {}
