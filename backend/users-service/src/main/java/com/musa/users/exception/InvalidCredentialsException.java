package com.musa.users.exception;

/**Excepción lanzada cuando las credenciales de inicio de sesión son incorrectas o inválidas.*/
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
