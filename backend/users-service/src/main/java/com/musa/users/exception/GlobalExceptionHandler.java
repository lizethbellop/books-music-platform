package com.musa.users.exception;

import com.musa.users.dto.response.MessageResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones del microservicio. Intercepta los errores lanzados en la capa de negocio
 * y los transforma en respuestas HTTP estandarizadas con mensajes claros para el cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    //Fallos de validación en DTOs (@Valid) -> Devuelve un MAPA para marcar los inputs en el frontend
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    // HTTP 400 BAD REQUEST: Violación de reglas de contraseña
    @ExceptionHandler({
            PasswordMismatchException.class,
            WeakPasswordException.class,
            SamePasswordException.class
    })
    public ResponseEntity<MessageResponseDto> handleBadRequestExceptions(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponseDto(exception.getMessage()));
    }

    // HTTP 401 UNAUTHORIZED: Credenciales y Tokens inválidos o expirados
    @ExceptionHandler({
            InvalidCredentialsException.class,
            InvalidTokenException.class,
            TokenExpiredException.class
    })
    public ResponseEntity<MessageResponseDto> handleUnauthorizedExceptions(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponseDto(exception.getMessage()));
    }

    // HTTP 403 FORBIDDEN: Cuenta deshabilitada o inactiva
    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<MessageResponseDto> handleAccountDisabled(AccountDisabledException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponseDto(exception.getMessage()));
    }

    // HTTP 404 NOT FOUND: Recurso no encontrado en BD
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<MessageResponseDto> handleResourceNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponseDto(exception.getMessage()));
    }

    // HTTP 409 CONFLICT: Duplicidad (correo ya registrado)
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<MessageResponseDto> handleUserAlreadyExists(UserAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new MessageResponseDto(exception.getMessage()));
    }

    // HTTP 500 INTERNAL SERVER ERROR: Fallo de infraestructura de correo
    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<MessageResponseDto> handleEmailSendError(EmailSendException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponseDto("Error en el servicio de correo: " + exception.getMessage()));
    }
}
