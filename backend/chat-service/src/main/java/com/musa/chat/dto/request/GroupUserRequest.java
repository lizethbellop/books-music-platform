package com.musa.chat.dto.request;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record GroupUserRequest(@NotNull UUID userId) {}
