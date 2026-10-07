package com.musa.users.exception;

/** Excepción lanzada cuando una contraseña no cumple con los requisitos de complejidad y seguridad. */
public class WeakPasswordException extends RuntimeException {
    public WeakPasswordException(String message) {
        super(message);
    }
}
