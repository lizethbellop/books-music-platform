package com.musa.users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO de solicitud para iniciar el proceso de recuperación de contraseña. */
public record ForgotPasswordRequestDto(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        String email
) { }