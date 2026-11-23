package com.musa.communities_service.polls.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record CreatePollRequest(

        @NotBlank(message = "Escribe una pregunta")
        @Size(max = 300, message = "La pregunta admite hasta 300 caracteres")
        String question,

        @NotNull(message = "Agrega las opciones")
        @Size(min = 2, max = 10,
                message = "La encuesta debe tener entre 2 y 10 opciones")
        List<
                @NotBlank(message = "Las opciones no pueden estar vacías")
                @Size(max = 150,
                        message = "Cada opción admite hasta 150 caracteres")
                        String
                > options,

        @NotNull(message = "Indica cuándo termina la encuesta")
        @Future(message = "El cierre debe ser una fecha futura")
        OffsetDateTime closesAt

) {
}