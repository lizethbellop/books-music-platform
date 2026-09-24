package com.musa.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserListRequest(

        @NotBlank(message = "List name is required")
        @Size(max = 100, message = "List name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
}