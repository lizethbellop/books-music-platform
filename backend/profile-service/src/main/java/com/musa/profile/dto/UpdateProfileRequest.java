package com.musa.profile.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest (

        @Size(max = 500, message = "Biography must not exceed 500 characters")
        String biography,

        @NotNull(message = "Privacy value is required")
        Boolean privateProfile
){
}
