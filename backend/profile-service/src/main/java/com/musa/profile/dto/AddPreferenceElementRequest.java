package com.musa.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddPreferenceElementRequest(

        @NotBlank(message = "Element type is required")
        @Pattern(
                regexp = "BOOK|SONG|ARTIST|AUTHOR",
                message = "Element type must be BOOK, SONG, ARTIST or AUTHOR"
        )
        String elementType,

        @NotBlank(message = "Reference ID is required")
        @Size(max = 255, message = "Reference ID must not exceed 255 characters")
        String referenceId
) {
}
