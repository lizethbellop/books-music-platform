package com.musa.statistics.dto;

public record MusicStatisticsResponse(
        long reviews,
        long ratings,
        long favorites,
        Double averageRating
) {
}