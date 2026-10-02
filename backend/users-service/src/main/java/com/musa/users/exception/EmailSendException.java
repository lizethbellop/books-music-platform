package com.musa.users.exception;

/**Excepción lanzada cuando ocurre un error técnico durante el envío del correo electrónico. */

public class EmailSendException extends RuntimeException {
    public EmailSendException(String message) {
        super(message);
    }
}
