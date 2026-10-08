package com.musa.books.controller;

import com.musa.books.dto.RatingRequest;
import com.musa.books.dto.ReviewRequest;
import com.musa.books.dto.ReviewResponse;
import com.musa.books.entity.Review;
import com.musa.books.service.ReviewService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.musa.books.config.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/books")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(
            ReviewService reviewService
    ) {
        this.reviewService = reviewService;
    }

    // Calificar libro
    @PutMapping("/{externalId}/rating")
    public ResponseEntity<Review> rateBook(
            @PathVariable String externalId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "userId", required = false) UUID suppliedUserId,
            @Valid @RequestBody RatingRequest request
    ) {
        UUID userId = AuthenticatedUser.requireOwn(jwt, suppliedUserId);


        Review review = reviewService.rateBook(
                userId,
                externalId,
                request.getRating()
        );

        return ResponseEntity.ok(review);
    }

    // Crear reseña
    @PostMapping("/{externalId}/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable String externalId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "userId", required = false) UUID suppliedUserId,
            @Valid@RequestBody ReviewRequest request
    ) {
        UUID userId = AuthenticatedUser.requireOwn(jwt, suppliedUserId);


        Review review = reviewService.createReview(
                userId,
                externalId,
                request.getRating(),
                request.getReviewText()
        );

        return ResponseEntity.ok(toResponse(review));
    }

    // Actualizar reseña
    @PutMapping("/{externalId}/reviews")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable String externalId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "userId", required = false) UUID suppliedUserId,
            @Valid @RequestBody ReviewRequest request
    ) {
        UUID userId = AuthenticatedUser.requireOwn(jwt, suppliedUserId);


        Review review = reviewService.updateReview(
                userId,
                externalId,
                request.getRating(),
                request.getReviewText()
        );

        return ResponseEntity.ok(toResponse(review));
    }

    // Eliminar reseña
    @DeleteMapping("/{externalId}/reviews")
    public ResponseEntity<Void> deleteReview(
            @PathVariable String externalId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "userId", required = false) UUID suppliedUserId
    ) {
        UUID userId = AuthenticatedUser.requireOwn(jwt, suppliedUserId);


        reviewService.deleteReview(
                userId,
                externalId
        );

        return ResponseEntity.noContent().build();
    }

    // Consultar reseñas
    @GetMapping("/{externalId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getReviews(
            @PathVariable String externalId
    ) {

        List<ReviewResponse> reviews =
                reviewService.getReviewsByBook(externalId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(reviews);
    }

    private ReviewResponse toResponse(Review review) {

        return new ReviewResponse(
                review.getUserId(),
                review.getBook().getExternalId(),
                review.getBook().getTitle(),
                review.getRating(),
                review.getReviewText(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}