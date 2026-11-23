package com.musa.communities_service.polls.exception;

public class InvalidPollRequestException extends RuntimeException {

    public InvalidPollRequestException(String message) {
        super(message);
    }
}