package com.musa.chat.exception;

public class SelfConversationNotAllowedException extends RuntimeException {

    public SelfConversationNotAllowedException() {
        super("No puedes crear una conversación privada contigo mismo.");
    }
}