package com.musa.profile.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProfileNotFoundException extends RuntimeException{
    public ProfileNotFoundException(UUID userId){
        super("Profile not found for user: " + userId);
    }
}
