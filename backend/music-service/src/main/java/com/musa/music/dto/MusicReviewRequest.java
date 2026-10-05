package com.musa.music.dto;

import com.musa.music.entity.MusicContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MusicReviewRequest(
        @NotNull Long userId,
        @NotBlank String spotifyId,
        @NotNull MusicContentType contentType,
        @NotBlank String name,
        String artistName,
        String imageUrl,
        String reviewText
) {
}