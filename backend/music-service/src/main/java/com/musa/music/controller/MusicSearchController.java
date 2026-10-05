package com.musa.music.controller;

import com.musa.music.dto.MusicSearchItemResponse;
import com.musa.music.service.MusicSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/music")
public class MusicSearchController {

    private final MusicSearchService musicSearchService;

    public MusicSearchController(
            MusicSearchService musicSearchService
    ) {
        this.musicSearchService = musicSearchService;
    }

    @GetMapping("/search")
    public List<MusicSearchItemResponse> searchMusic(
            @RequestParam String q
    ) {
        return musicSearchService.search(q);
    }
}