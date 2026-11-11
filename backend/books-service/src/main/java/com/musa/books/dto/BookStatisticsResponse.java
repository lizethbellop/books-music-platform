package com.musa.books.dto;

import java.math.BigDecimal;

public record BookStatisticsResponse(
        long reviews,
        BigDecimal averageRating,
        long read,
        long reading,
        long toRead
) {
}