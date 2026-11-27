package com.musa.chat.dto.request;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
public record CreateGroupRequest(@NotBlank @Size(max = 100) String name,
        @NotNull @Size(min = 1, max = 49) List<@NotNull UUID> participantIds) {}
