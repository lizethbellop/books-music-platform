package com.musa.profile.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PreferenceElementAlreadyExistsException extends RuntimeException {

    public PreferenceElementAlreadyExistsException() {
        super("This item is already in the user's preferences");
    }
}
