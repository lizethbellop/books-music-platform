package com.musa.books.service;

import com.musa.books.entity.Book;
import com.musa.books.entity.Review;
import com.musa.books.exception.ResourceNotFoundException;
import com.musa.books.repository.ReviewRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {

    private static final BigDecimal MIN_RATING =
            new BigDecimal("0.5");

    private static final BigDecimal MAX_RATING =
            new BigDecimal("5.0");

    private static final BigDecimal HALF_STAR =
            new BigDecimal("0.5");

    private final ReviewRepository reviewRepository;
    private final BookService bookService;

    public ReviewService(
            ReviewRepository reviewRepository,
            BookService bookService
    ) {
        this.reviewRepository = reviewRepository;
        this.bookService = bookService;
    }

    // CU-13 Calificar libro
    public Review rateBook(
            UUID userId,
            String externalId,
            BigDecimal rating
    ) {

        validateRating(rating);

        Book book = bookService.getOrCreateBook(externalId);

        Review review = reviewRepository
                .findByUserIdAndBook_Id(
                        userId,
                        book.getId()
                )
                .orElseGet(() ->
                        Review.builder()
                                .userId(userId)
                                .book(book)
                                .build()
                );

        review.setRating(rating);

        return reviewRepository.save(review);
    }

    // CU-16 Crear reseña
    public Review createReview(
            UUID userId,
            String externalId,
            BigDecimal rating,
            String reviewText
    ) {

        validateRating(rating);

        Book book = bookService.getOrCreateBook(externalId);

        boolean alreadyExists = reviewRepository
                .findByUserIdAndBook_Id(
                        userId,
                        book.getId()
                )
                .isPresent();

        if (alreadyExists) {
            throw new IllegalStateException(
                    "El usuario ya tiene una reseña para este libro"
            );
        }

        Review review = Review.builder()
                .userId(userId)
                .book(book)
                .rating(rating)
                .reviewText(reviewText)
                .build();

        return reviewRepository.save(review);
    }

    // CU-15 Actualizar reseña
    public Review updateReview(
            UUID userId,
            String externalId,
            BigDecimal rating,
            String reviewText
    ) {

        validateRating(rating);

        Book book = bookService.getOrCreateBook(externalId);

        Review review = reviewRepository
                .findByUserIdAndBook_Id(
                        userId,
                        book.getId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "La reseña no existe"
                        )
                );

        review.setRating(rating);
        review.setReviewText(reviewText);

        return reviewRepository.save(review);
    }

    // CU-17 Eliminar reseña
    public void deleteReview(
            UUID userId,
            String externalId
    ) {

        Book book = bookService.getOrCreateBook(externalId);

        Review review = reviewRepository
                .findByUserIdAndBook_Id(
                        userId,
                        book.getId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("La reseña no existe")                            
                        );
                        
        reviewRepository.delete(review);
    }

    // CU-18 Consultar reseñas
    public List<Review> getReviewsByBook(
            String externalId
    ) {

        Book book = bookService.getOrCreateBook(externalId);

        return reviewRepository.findByBook_Id(
                book.getId()
        );
    }

    private void validateRating(
            BigDecimal rating
    ) {

        if (rating == null) {
            throw new IllegalArgumentException(
                    "La calificación es obligatoria"
            );
        }

        if (rating.compareTo(MIN_RATING) < 0
                || rating.compareTo(MAX_RATING) > 0) {

            throw new IllegalArgumentException(
                    "La calificación debe estar entre 0.5 y 5.0"
            );
        }

        if (rating.remainder(HALF_STAR)
                .compareTo(BigDecimal.ZERO) != 0) {

            throw new IllegalArgumentException(
                    "La calificación debe avanzar en incrementos de 0.5"
            );
        }
    }
}