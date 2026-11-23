package com.musa.chat.exception;

import com.musa.chat.dto.response.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SelfConversationNotAllowedException.class)
    public ResponseEntity<ApiError> selfConversation(SelfConversationNotAllowedException exception) {
        return error(HttpStatus.BAD_REQUEST, "SELF_CONVERSATION_NOT_ALLOWED", exception.getMessage());
    }

    @ExceptionHandler(RecipientUnavailableException.class)
    public ResponseEntity<ApiError> recipientUnavailable(RecipientUnavailableException exception) {
        return error(HttpStatus.NOT_FOUND, "RECIPIENT_UNAVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(ConversationBusyException.class)
    public ResponseEntity<ApiError> conversationBusy(ConversationBusyException exception) {
        return unavailable("CONVERSATION_BUSY", exception.getMessage());
    }

    @ExceptionHandler(UsersServiceUnavailableException.class)
    public ResponseEntity<ApiError> usersUnavailable(UsersServiceUnavailableException exception) {
        log.warn("No se pudo verificar participantes: {}", exception.getCause().getClass().getSimpleName());
        return unavailable("USERS_SERVICE_UNAVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(new ApiError("UNAUTHORIZED", "Se requiere un token de acceso válido."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden() {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "No tienes permiso para esta operación.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidFields() {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Revisa los datos: nombre, participantes y contenido deben cumplir los límites permitidos.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadableBody() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Envía un JSON válido con identificadores en formato UUID.");
    }

    @ExceptionHandler({CannotCreateTransactionException.class,
            DataAccessResourceFailureException.class, QueryTimeoutException.class})
    public ResponseEntity<ApiError> databaseUnavailable(Exception exception) {
        log.warn("Persistencia temporalmente no disponible: {}", exception.getClass().getSimpleName());
        return unavailable("CHAT_STORAGE_UNAVAILABLE", "No podemos acceder al chat en este momento. Inténtalo de nuevo.");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> persistenceError(DataAccessException exception) {
        log.error("Error interno de persistencia del chat", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "No pudimos completar la operación.");
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ApiError> conversationNotFound(ConversationNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "CONVERSATION_UNAVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(InvalidChatRequestException.class)
    public ResponseEntity<ApiError> invalidChatRequest(InvalidChatRequestException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
    }

    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> invalidParameter(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Revisa los parámetros de la solicitud.");
    }

    @ExceptionHandler(GroupPermissionException.class)
    public ResponseEntity<ApiError> groupPermission(GroupPermissionException exception) {
        return error(HttpStatus.FORBIDDEN, "GROUP_ADMIN_REQUIRED", exception.getMessage());
    }

    private ResponseEntity<ApiError> unavailable(String code, String message) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "5")
                .body(new ApiError(code, message));
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }
}
