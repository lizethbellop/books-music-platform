package com.musa.notifications.exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.Map;
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound() {
        return error(HttpStatus.NOT_FOUND, "NOTIFICATION_UNAVAILABLE", "Notificación no disponible.");
    }
    @ExceptionHandler({InvalidNotificationQueryException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> invalidQuery() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Revisa los parámetros de la solicitud.");
    }
    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class, QueryTimeoutException.class})
    public ResponseEntity<Map<String, String>> unavailable(Exception exception) {
        log.warn("Almacenamiento de notificaciones no disponible: {}", exception.getClass().getSimpleName());
        return ResponseEntity.status(503).header(HttpHeaders.RETRY_AFTER, "5")
                .body(Map.of("code", "NOTIFICATIONS_UNAVAILABLE", "message", "Las notificaciones no están disponibles en este momento."));
    }
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> databaseError(DataAccessException exception) {
        log.error("Error interno de persistencia de notificaciones", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "No pudimos completar la operación.");
    }
    private ResponseEntity<Map<String, String>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("code", code, "message", message));
    }
}
