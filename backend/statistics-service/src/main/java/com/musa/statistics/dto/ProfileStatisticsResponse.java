package com.musa.statistics.dto;

public record ProfileStatisticsResponse(
        long lists,
        long listElements,
        long preferenceElements
) {
}