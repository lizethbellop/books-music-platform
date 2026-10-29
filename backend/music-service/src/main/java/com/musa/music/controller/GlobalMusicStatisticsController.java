package com.musa.music.controller;

import com.musa.music.dto.GlobalMusicStatisticsResponse;
import com.musa.music.service.GlobalMusicStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/music/statistics")
public class GlobalMusicStatisticsController {

    private final GlobalMusicStatisticsService globalMusicStatisticsService;

    public GlobalMusicStatisticsController(
            GlobalMusicStatisticsService globalMusicStatisticsService
    ) {
        this.globalMusicStatisticsService =
                globalMusicStatisticsService;
    }

    @GetMapping("/global")
    public GlobalMusicStatisticsResponse getGlobalStatistics() {
        return globalMusicStatisticsService
                .getGlobalStatistics();
    }
}