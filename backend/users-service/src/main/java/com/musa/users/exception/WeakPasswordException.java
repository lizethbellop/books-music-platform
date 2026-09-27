package com.musa.users.exception;

/**
 * CU-01 (RN-SEC-01) y CU-05 (FA 7.1 / RN-SEC-17) - La contraseña no cumple con la
 * complejidad requerida (mínimo 8 caracteres, mayúscula, minúscula, número y carácter especial).
 * */
public class WeakPasswordException extends RuntimeException {
    public WeakPasswordException(String message) {
        super(message);
    }
}
