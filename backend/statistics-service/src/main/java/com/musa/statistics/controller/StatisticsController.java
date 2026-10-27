package com.musa.statistics.controller;

import java.util.Map;
import java.util.UUID;

import com.musa.statistics.dto.PersonalStatisticsResponse;
import com.musa.statistics.service.PersonalStatisticsService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final PersonalStatisticsService personalStatisticsService;

    public StatisticsController(
            PersonalStatisticsService personalStatisticsService
    ) {
        this.personalStatisticsService =
                personalStatisticsService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "statistics-service",
                "status", "UP"
        );
    }

    @GetMapping("/personal")
    public PersonalStatisticsResponse getPersonalStatistics(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorizationHeader
    ) {
        UUID userId =
                UUID.fromString(jwt.getSubject());

        return personalStatisticsService
                .getPersonalStatistics(
                        userId,
                        authorizationHeader
                );
    }
}