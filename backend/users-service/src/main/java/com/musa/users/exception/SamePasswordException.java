package com.musa.users.exception;

/**
 * CU-05 (RN-SEC-22) - La nueva contraseña ingresada no puede ser idéntica a la contraseña
 * registrada actualmente.
 * */

public class SamePasswordException extends RuntimeException {
    public SamePasswordException(String message) {
        super(message);
    }
}
