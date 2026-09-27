package com.musa.users.exception;

/**
 * CU-01 (FA 3.2) - Se detecta que el correo electrónico ingresado ya se encuentra asociado
 * a una cuenta existente.
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}