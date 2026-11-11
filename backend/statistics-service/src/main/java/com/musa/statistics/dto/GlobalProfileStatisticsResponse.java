package com.musa.statistics.dto;

public record GlobalProfileStatisticsResponse(
        long lists,
        long listElements,
        long preferenceElements
) {
}