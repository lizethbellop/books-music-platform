package com.musa.notifications.exception;
public class InvalidNotificationQueryException extends RuntimeException {
    public InvalidNotificationQueryException() { super("Página o tamaño no válido. Usa tamaños de 1 a 50."); }
}
