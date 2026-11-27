package com.musa.chat.dto.request;
import jakarta.validation.constraints.*;
public record GroupNameRequest(@NotBlank @Size(max = 100) String name) {}
