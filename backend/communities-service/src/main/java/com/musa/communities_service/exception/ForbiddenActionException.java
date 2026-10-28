package com.musa.communities_service.exception;

/**
 * Excepción lanzada cuando el usuario autenticado intenta ejecutar una acción
 * para la cual no posee los permisos o rol requerido dentro de la comunidad.
 */
public class ForbiddenActionException extends RuntimeException {
    public ForbiddenActionException(String message) {
        super(message);
    }
}