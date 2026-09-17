package com.musa.music.dto;

import com.musa.music.entity.MusicContentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MusicRatingRequest(

        @NotNull
        Long userId,

        @NotBlank
        String spotifyId,

        @NotNull
        MusicContentType contentType,

        @NotBlank
        String name,

        String artistName,

        String imageUrl,

        @NotNull
        @Min(1)
        @Max(5)
        Double rating
) {
}