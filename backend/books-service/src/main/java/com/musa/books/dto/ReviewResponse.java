package com.musa.books.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class ReviewResponse {

    private UUID userId;
    private String externalId;
    private String title;
    private BigDecimal rating;
    private String reviewText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReviewResponse(
            UUID userId,
            String externalId,
            String title,
            BigDecimal rating,
            String reviewText,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.userId = userId;
        this.externalId = externalId;
        this.title = title;
        this.rating = rating;
        this.reviewText = reviewText;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}