package com.musa.social.exception;

public class UnprocessableEntityException
        extends RuntimeException {

    public UnprocessableEntityException(String message) {
        super(message);
    }
}