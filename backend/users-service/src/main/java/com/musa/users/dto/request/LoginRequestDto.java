package com.musa.users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO de solicitud para la autenticación e inicio de sesión de usuarios. */
public record LoginRequestDto(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password,

        Boolean rememberMe
) {
    public LoginRequestDto {
        if (rememberMe == null) {
            rememberMe = false;
        }
    }
}