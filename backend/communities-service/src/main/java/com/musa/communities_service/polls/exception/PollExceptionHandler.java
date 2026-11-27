package com.musa.communities_service.polls.exception;

import com.musa.communities_service.dto.response.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(
        basePackages = "com.musa.communities_service.polls.controller"
)
public class PollExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(PollExceptionHandler.class);

    @ExceptionHandler(InvalidPollRequestException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidRequest(
            InvalidPollRequestException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(error(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request
        ));
    }

    @ExceptionHandler({
            PessimisticLockingFailureException.class,
            QueryTimeoutException.class,
            TransactionTimedOutException.class
    })
    public ResponseEntity<ErrorResponseDto> handleBusyDatabase(
            Exception exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Tiempo de espera agotado en encuestas: {}",
                exception.getClass().getSimpleName()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "2")
                .body(error(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "No pudimos confirmar la operación. "
                                + "Espera un momento y consulta su estado.",
                        request
                ));
    }

    @ExceptionHandler({
            CannotCreateTransactionException.class,
            DataAccessResourceFailureException.class
    })
    public ResponseEntity<ErrorResponseDto> handleUnavailableDatabase(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Base de datos no disponible para encuestas: {}",
                exception.getClass().getSimpleName()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "5")
                .body(error(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "El servicio está temporalmente no disponible. "
                                + "Intenta nuevamente en unos momentos.",
                        request
                ));
    }

    @ExceptionHandler(PollCapacityException.class)
    public ResponseEntity<ErrorResponseDto> handleCapacity(
            PollCapacityException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "5")
                .body(error(
                        HttpStatus.TOO_MANY_REQUESTS,
                        exception.getMessage(),
                        request
                ));
    }

    private ErrorResponseDto error(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return new ErrorResponseDto(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                null
        );
    }
}