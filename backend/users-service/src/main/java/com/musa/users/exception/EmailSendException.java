package com.musa.users.exception;

/**
 * CU-04 (EX-2) - Falla técnica al intentar enviar el correo electrónico de recuperación.
 * */

public class EmailSendException extends RuntimeException {
    public EmailSendException(String message) {
        super(message);
    }
}
