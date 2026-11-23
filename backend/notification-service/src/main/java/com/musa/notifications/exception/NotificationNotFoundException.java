package com.musa.notifications.exception;
public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException() { super("Notificación no disponible."); }
}
