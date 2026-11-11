package com.musa.books.service;

import java.math.BigDecimal;
import java.util.UUID;

import com.musa.books.dto.BookStatisticsResponse;
import com.musa.books.enums.ReadingStatus;
import com.musa.books.repository.ReviewRepository;
import com.musa.books.repository.UserBookRepository;
import org.springframework.stereotype.Service;

@Service
public class BookStatisticsService {

    private final ReviewRepository reviewRepository;
    private final UserBookRepository userBookRepository;

    public BookStatisticsService(
            ReviewRepository reviewRepository,
            UserBookRepository userBookRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.userBookRepository = userBookRepository;
    }

    public BookStatisticsResponse getUserStatistics(
            UUID userId
    ) {
        long reviews =
                reviewRepository.countByUserId(userId);

        BigDecimal averageRating =
                reviewRepository
                        .findAverageRatingByUserId(userId);

        long read =
                userBookRepository
                        .countByUserIdAndReadingStatus(
                                userId,
                                ReadingStatus.LEIDO
                        );

        long reading =
                userBookRepository
                        .countByUserIdAndReadingStatus(
                                userId,
                                ReadingStatus.LEYENDO
                        );

        long toRead =
                userBookRepository
                        .countByUserIdAndReadingStatus(
                                userId,
                                ReadingStatus.POR_LEER
                        );

        return new BookStatisticsResponse(
                reviews,
                averageRating,
                read,
                reading,
                toRead
        );
    }
}