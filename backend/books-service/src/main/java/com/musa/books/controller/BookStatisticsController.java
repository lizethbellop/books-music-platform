package com.musa.books.controller;

import java.util.UUID;

import com.musa.books.dto.BookStatisticsResponse;
import com.musa.books.service.BookStatisticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books/statistics")
public class BookStatisticsController {

    private final BookStatisticsService bookStatisticsService;

    public BookStatisticsController(
            BookStatisticsService bookStatisticsService
    ) {
        this.bookStatisticsService =
                bookStatisticsService;
    }

    @GetMapping("/me")
    public BookStatisticsResponse getMyStatistics(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId =
                UUID.fromString(jwt.getSubject());

        return bookStatisticsService
                .getUserStatistics(userId);
    }
}