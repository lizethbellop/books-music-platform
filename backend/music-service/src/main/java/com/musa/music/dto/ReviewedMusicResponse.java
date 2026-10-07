package com.musa.music.dto;

import com.musa.music.entity.MusicContentType;

import java.time.LocalDateTime;

public record ReviewedMusicResponse(
        String spotifyId,
        MusicContentType contentType,
        String name,
        String artistName,
        String imageUrl,
        Double rating,
        LocalDateTime reviewDate
) {
}