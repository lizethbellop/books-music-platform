package com.musa.music.dto;

import java.util.UUID;

import com.musa.music.entity.MusicContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MusicFavoriteRequest(

        @NotNull
        UUID userId,

        @NotBlank
        String spotifyId,

        @NotNull
        MusicContentType contentType,

        @NotBlank
        String name,

        String artistName,

        String imageUrl
) {
}