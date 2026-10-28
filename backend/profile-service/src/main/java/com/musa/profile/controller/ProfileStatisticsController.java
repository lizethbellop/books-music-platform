package com.musa.profile.controller;

import java.util.UUID;

import com.musa.profile.dto.ProfileStatisticsResponse;
import com.musa.profile.service.ProfileStatisticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles/statistics")
public class ProfileStatisticsController {

    private final ProfileStatisticsService profileStatisticsService;

    public ProfileStatisticsController(
            ProfileStatisticsService profileStatisticsService
    ) {
        this.profileStatisticsService =
                profileStatisticsService;
    }

    @GetMapping("/me")
    public ProfileStatisticsResponse getMyStatistics(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId =
                UUID.fromString(jwt.getSubject());

        return profileStatisticsService
                .getUserStatistics(userId);
    }
}