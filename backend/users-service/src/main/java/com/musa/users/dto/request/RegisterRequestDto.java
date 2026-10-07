package com.musa.users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTO de solicitud para el registro de nuevos usuarios en el sistema. */
public record RegisterRequestDto(
        @NotBlank(message = "El nombre es obligatorio")
        String fullName,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$", message = "La contraseña debe incluir al menos un número, una mayúscula, una minúscula y un carácter especial")
        String password,

        @NotBlank(message = "La confirmación de contraseña es obligatoria")
        String confirmPassword,

        @NotBlank(message = "Debe seleccionar un tipo de cuenta (rol)")
        String roleName
) {}