package com.musa.statistics.controller;

import java.util.UUID;

import com.musa.statistics.dto.GlobalStatisticsResponse;
import com.musa.statistics.dto.PersonalStatisticsResponse;
import com.musa.statistics.service.GlobalStatisticsService;
import com.musa.statistics.service.PersonalStatisticsService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final PersonalStatisticsService personalStatisticsService;
    private final GlobalStatisticsService globalStatisticsService;

    public StatisticsController(
            PersonalStatisticsService personalStatisticsService,
            GlobalStatisticsService globalStatisticsService
    ) {
        this.personalStatisticsService =
                personalStatisticsService;

        this.globalStatisticsService =
                globalStatisticsService;
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

    @GetMapping("/global")
    public GlobalStatisticsResponse getGlobalStatistics(
            @RequestHeader(HttpHeaders.AUTHORIZATION)
            String authorizationHeader
    ) {
        return globalStatisticsService
                .getGlobalStatistics(
                        authorizationHeader
                );
    }
}