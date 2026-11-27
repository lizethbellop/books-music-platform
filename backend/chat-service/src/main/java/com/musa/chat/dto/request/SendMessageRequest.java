package com.musa.chat.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record SendMessageRequest(
        @NotBlank(message = "Escribe un mensaje.")
        @Size(max = 4000, message = "El mensaje admite hasta 4000 caracteres.") String content) {}
