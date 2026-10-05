package com.musa.profile.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PreferenceElementNotFoundException extends RuntimeException {

    public PreferenceElementNotFoundException(UUID elementId) {
        super("Preference element not found: " + elementId);
    }
}