package com.musa.profile.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ListElementAlreadyExistsException extends RuntimeException {

    public ListElementAlreadyExistsException() {
        super("This item is already in the list");
    }
}
