package com.musa.music.dto;

import com.musa.music.entity.MusicContentType;

public record MusicSearchItemResponse(
        String spotifyId,
        MusicContentType contentType,
        String name,
        String artistName,
        String imageUrl
) {
}