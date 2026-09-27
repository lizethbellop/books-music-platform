package com.musa.users.exception;

/**
 * CU-03 (FA 4.1) y CU-05 (FA 3.1 / RN-SEC-13) - El token o sesión ha superado su vigencia máxima de tiempo.
 * */

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}
