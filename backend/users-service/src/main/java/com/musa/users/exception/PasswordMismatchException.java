package com.musa.users.exception;

/**Excepción lanzada cuando la contraseña ingresada y su confirmación no coinciden*/
public class PasswordMismatchException extends RuntimeException {
    public PasswordMismatchException(String message) {
        super(message);
    }
}
