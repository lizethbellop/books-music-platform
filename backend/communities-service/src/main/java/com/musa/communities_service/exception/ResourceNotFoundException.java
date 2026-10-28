package com.musa.communities_service.exception;
/**
 * Excepción lanzada cuando un recurso solicitado (Comunidad, Actividad, Membresía)
 * no es localizado en la base de datos.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
