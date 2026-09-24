package com.musa.profile.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ListElementNotFoundException extends RuntimeException {

  public ListElementNotFoundException(UUID elementId) {
    super("List element not found: " + elementId);
  }
}