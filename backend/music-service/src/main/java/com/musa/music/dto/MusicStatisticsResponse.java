package com.musa.music.dto;

public record MusicStatisticsResponse(
        long reviews,
        long ratings,
        long favorites,
        Double averageRating
) {
}