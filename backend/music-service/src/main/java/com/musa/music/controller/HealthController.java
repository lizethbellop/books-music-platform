package com.musa.music.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/music")
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "music-service is running";
    }
}