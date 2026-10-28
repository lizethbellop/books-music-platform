package com.musa.communities_service.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO de respuesta estandarizado para la entrega de errores en la API REST.
 */
public record ErrorResponseDto(
        int status,
        String error,
        String message,
        String path,
        OffsetDateTime timestamp,
        List<String> details
) {}