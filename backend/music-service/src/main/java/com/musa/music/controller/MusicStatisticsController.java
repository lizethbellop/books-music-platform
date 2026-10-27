package com.musa.music.controller;

import java.util.UUID;

import com.musa.music.dto.MusicStatisticsResponse;
import com.musa.music.service.MusicStatisticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/music/statistics")
public class MusicStatisticsController {

    private final MusicStatisticsService musicStatisticsService;

    public MusicStatisticsController(
            MusicStatisticsService musicStatisticsService
    ) {
        this.musicStatisticsService =
                musicStatisticsService;
    }

    @GetMapping("/me")
    public MusicStatisticsResponse getMyStatistics(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId =
                UUID.fromString(jwt.getSubject());

        return musicStatisticsService
                .getUserStatistics(userId);
    }
}