package com.musa.music.dto;

public record GlobalMusicStatisticsResponse(
        long reviews,
        long ratings,
        long favorites,
        Double averageRating
) {
}