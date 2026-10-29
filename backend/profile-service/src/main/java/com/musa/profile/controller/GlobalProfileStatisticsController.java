package com.musa.profile.controller;

import com.musa.profile.dto.GlobalProfileStatisticsResponse;
import com.musa.profile.service.GlobalProfileStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles/statistics")
public class GlobalProfileStatisticsController {

    private final GlobalProfileStatisticsService globalProfileStatisticsService;

    public GlobalProfileStatisticsController(
            GlobalProfileStatisticsService globalProfileStatisticsService
    ) {
        this.globalProfileStatisticsService =
                globalProfileStatisticsService;
    }

    @GetMapping("/global")
    public GlobalProfileStatisticsResponse getGlobalStatistics() {
        return globalProfileStatisticsService
                .getGlobalStatistics();
    }
}