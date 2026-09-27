package com.musa.users.exception;

/**
 *Excepción base para búsquedas fallidas de entidades en base de datos (ej. Rol no configurado en BD).
 */

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
