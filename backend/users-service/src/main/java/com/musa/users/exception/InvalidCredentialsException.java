package com.musa.users.exception;

/**
 * CU-02 (FA 3.2, FA 5.1) - Credenciales erróneas o usuario no registrado.
 * Cumple la regla RN-SEC-05 (el sistema no revela si el error es el correo o la contraseña).
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
