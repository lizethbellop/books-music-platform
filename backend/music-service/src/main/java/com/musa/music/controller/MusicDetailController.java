package com.musa.music.controller;

import com.musa.music.dto.MusicDetailResponse;
import com.musa.music.entity.MusicContentType;
import com.musa.music.service.MusicDetailService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/music")
public class MusicDetailController {

    private final MusicDetailService musicDetailService;

    public MusicDetailController(
            MusicDetailService musicDetailService
    ) {
        this.musicDetailService = musicDetailService;
    }

    @GetMapping("/{type}/{spotifyId}")
    public MusicDetailResponse getMusicDetail(
            @PathVariable String type,
            @PathVariable String spotifyId
    ) {

        MusicContentType contentType =
                MusicContentType.valueOf(type.toUpperCase());

        return musicDetailService.getDetail(
                contentType,
                spotifyId
        );
    }
}