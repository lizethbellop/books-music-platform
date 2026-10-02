package com.musa.users.exception;

/**Excepción lanzada cuando la cuenta del usuario se encuentra inactiva o suspendida.*/
public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException(String message) {
        super(message);
    }
}
