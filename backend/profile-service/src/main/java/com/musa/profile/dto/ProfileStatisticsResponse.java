package com.musa.profile.dto;

public record ProfileStatisticsResponse(
        long lists,
        long listElements,
        long preferenceElements
) {
}