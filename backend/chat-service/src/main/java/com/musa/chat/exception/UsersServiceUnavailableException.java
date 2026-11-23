package com.musa.chat.exception;

public class UsersServiceUnavailableException extends RuntimeException {
    public UsersServiceUnavailableException(Throwable cause) {
        super("No podemos verificar los participantes en este momento. Inténtalo de nuevo.", cause);
    }
}
