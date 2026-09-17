package com.musa.music.controller;

import com.musa.music.dto.MusicDetailResponse;
import com.musa.music.entity.MusicContentType;
import com.musa.music.spotify.SpotifyDetailService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/music")
public class MusicDetailController {

    private final SpotifyDetailService spotifyDetailService;

    public MusicDetailController(
            SpotifyDetailService spotifyDetailService
    ) {
        this.spotifyDetailService = spotifyDetailService;
    }

    @GetMapping("/{type}/{spotifyId}")
    public MusicDetailResponse getMusicDetail(
            @PathVariable String type,
            @PathVariable String spotifyId
    ) {

        MusicContentType contentType =
                MusicContentType.valueOf(type.toUpperCase());

        return spotifyDetailService.getDetail(
                contentType,
                spotifyId
        );
    }
}