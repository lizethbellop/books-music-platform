package com.musa.users.exception;

/**
 * CU-05 (FA 3.2 / RN-SEC-19, RN-SEC-20) - El token de recuperación no existe, fue alterado o ya fue utilizado.
 * */

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
