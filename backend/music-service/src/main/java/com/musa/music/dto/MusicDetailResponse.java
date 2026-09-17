package com.musa.music.dto;

import com.musa.music.entity.MusicContentType;

public record MusicDetailResponse(
        String spotifyId,
        MusicContentType contentType,
        String name,
        String artistName,
        String imageUrl,
        String spotifyUrl,
        String releaseDate,
        Integer totalTracks
) {
}