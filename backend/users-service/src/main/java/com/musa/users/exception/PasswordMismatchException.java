package com.musa.users.exception;

/**
 * CU-01 (FA 3.3) y CU-05 (FA 7.2) - La contraseña ingresada y la confirmación no coinciden.
 * */
public class PasswordMismatchException extends RuntimeException {
    public PasswordMismatchException(String message) {
        super(message);
    }
}
