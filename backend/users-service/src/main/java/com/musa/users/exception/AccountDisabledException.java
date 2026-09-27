package com.musa.users.exception;

/**
 * CU-02 (FA 6.1) - La cuenta del usuario se encuentra inactiva o suspendida.
 * */
public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException(String message) {
        super(message);
    }
}
