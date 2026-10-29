package com.musa.statistics.dto;

public record GlobalStatisticsResponse(
        GlobalMusicStatisticsResponse music,
        GlobalBookStatisticsResponse books,
        GlobalProfileStatisticsResponse profile
) {
}