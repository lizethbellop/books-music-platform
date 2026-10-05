package com.musa.books.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class RatingRequest {

    @NotNull(message = "La calificación es obligatoria")
    @DecimalMin(value = "0.5", message = "La calificación mínima es 0.5")
    @DecimalMax(value = "5.0", message = "La calificación máxima es 5.0")
    private BigDecimal rating;

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }
}