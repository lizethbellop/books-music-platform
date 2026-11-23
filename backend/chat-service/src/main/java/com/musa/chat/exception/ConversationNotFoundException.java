package com.musa.chat.exception;
public class ConversationNotFoundException extends RuntimeException {
    public ConversationNotFoundException() { super("Conversación no disponible."); }
}
