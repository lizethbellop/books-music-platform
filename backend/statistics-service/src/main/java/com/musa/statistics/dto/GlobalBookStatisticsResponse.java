package com.musa.statistics.dto;

import java.math.BigDecimal;

public record GlobalBookStatisticsResponse(
        long reviews,
        BigDecimal averageRating,
        long read,
        long reading,
        long toRead
) {
}