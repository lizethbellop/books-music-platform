package com.musa.users.exception;

/**Excepción lanzada cuando un token de autenticación o recuperación no existe, es inválido o ya fue utilizado.*/

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
