package com.musa.users.exception;

/**Excepción lanzada cuando un token de autenticación o recuperación ha superado su tiempo de validez.*/

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}
