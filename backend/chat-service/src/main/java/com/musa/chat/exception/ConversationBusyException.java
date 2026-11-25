package com.musa.chat.exception;

public class ConversationBusyException extends RuntimeException {

    public ConversationBusyException(Throwable cause) {
        super(
                "No pudimos abrir la conversación en este momento. Inténtalo de nuevo.",
                cause
        );
    }
}