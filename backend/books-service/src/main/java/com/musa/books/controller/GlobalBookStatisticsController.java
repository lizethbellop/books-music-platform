package com.musa.books.controller;

import com.musa.books.dto.GlobalBookStatisticsResponse;
import com.musa.books.service.GlobalBookStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books/statistics")
public class GlobalBookStatisticsController {

    private final GlobalBookStatisticsService globalBookStatisticsService;

    public GlobalBookStatisticsController(
            GlobalBookStatisticsService globalBookStatisticsService
    ) {
        this.globalBookStatisticsService =
                globalBookStatisticsService;
    }

    @GetMapping("/global")
    public GlobalBookStatisticsResponse getGlobalStatistics() {
        return globalBookStatisticsService
                .getGlobalStatistics();
    }
}