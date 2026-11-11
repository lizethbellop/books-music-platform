package com.musa.profile.dto;

public record GlobalProfileStatisticsResponse(
        long lists,
        long listElements,
        long preferenceElements
) {
}