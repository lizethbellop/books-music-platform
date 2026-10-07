package com.musa.users.exception;

/** Excepción lanzada cuando se intenta registrar un correo electrónico que ya existe en el sistema. */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}