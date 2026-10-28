package com.musa.books.service;

import java.math.BigDecimal;

import com.musa.books.dto.GlobalBookStatisticsResponse;
import com.musa.books.enums.ReadingStatus;
import com.musa.books.repository.ReviewRepository;
import com.musa.books.repository.UserBookRepository;
import org.springframework.stereotype.Service;

@Service
public class GlobalBookStatisticsService {

    private final ReviewRepository reviewRepository;
    private final UserBookRepository userBookRepository;

    public GlobalBookStatisticsService(
            ReviewRepository reviewRepository,
            UserBookRepository userBookRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.userBookRepository = userBookRepository;
    }

    public GlobalBookStatisticsResponse getGlobalStatistics() {

        long reviews = reviewRepository.count();

        BigDecimal averageRating =
                reviewRepository.findGlobalAverageRating();

        long read =
                userBookRepository.countByReadingStatus(
                        ReadingStatus.LEIDO
                );

        long reading =
                userBookRepository.countByReadingStatus(
                        ReadingStatus.LEYENDO
                );

        long toRead =
                userBookRepository.countByReadingStatus(
                        ReadingStatus.POR_LEER
                );

        return new GlobalBookStatisticsResponse(
                reviews,
                averageRating,
                read,
                reading,
                toRead
        );
    }
}