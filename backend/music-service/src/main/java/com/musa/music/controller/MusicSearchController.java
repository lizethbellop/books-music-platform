package com.musa.music.controller;

import com.musa.music.dto.MusicSearchItemResponse;
import com.musa.music.spotify.SpotifySearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/music")
public class MusicSearchController {

    private final SpotifySearchService spotifySearchService;

    public MusicSearchController(
            SpotifySearchService spotifySearchService
    ) {
        this.spotifySearchService = spotifySearchService;
    }

    @GetMapping("/search")
    public List<MusicSearchItemResponse> searchMusic(
            @RequestParam String q
    ) {
        return spotifySearchService.search(q);
    }
}