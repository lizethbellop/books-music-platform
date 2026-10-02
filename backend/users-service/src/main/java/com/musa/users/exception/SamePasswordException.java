package com.musa.users.exception;

/**Excepción lanzada cuando la nueva contraseña es idéntica a la contraseña registrada actualmente.*/

public class SamePasswordException extends RuntimeException {
    public SamePasswordException(String message) {
        super(message);
    }
}
