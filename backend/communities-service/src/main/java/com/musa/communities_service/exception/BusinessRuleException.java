package com.musa.communities_service.exception;
/**
 * Excepción lanzada cuando una operación viola una regla de negocio de la plataforma
 * (ej. intentar unirse dos veces a la misma comunidad o salir siendo el único administrador).
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
