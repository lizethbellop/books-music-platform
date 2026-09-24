package com.musa.profile.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserListNotFoundException extends RuntimeException {

    public UserListNotFoundException(UUID listId) {
        super("List not found: " + listId);
    }
}