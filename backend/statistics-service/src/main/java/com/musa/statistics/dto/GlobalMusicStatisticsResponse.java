package com.musa.statistics.dto;

public record GlobalMusicStatisticsResponse(
        long reviews,
        long ratings,
        long favorites,
        Double averageRating
) {
}