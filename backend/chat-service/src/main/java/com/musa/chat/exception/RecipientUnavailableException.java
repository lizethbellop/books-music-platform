package com.musa.chat.exception;

public class RecipientUnavailableException extends RuntimeException {
    public RecipientUnavailableException() {
        super("No se puede iniciar una conversación con ese usuario.");
    }
}
