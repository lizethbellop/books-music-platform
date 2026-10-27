package com.musa.statistics.dto;

import java.util.UUID;

public record PersonalStatisticsResponse(
        UUID userId,
        MusicStatisticsResponse music,
        BookStatisticsResponse books
) {
}