package com.musa.music.controller;

import com.musa.music.dto.MusicFavoriteRequest;
import com.musa.music.dto.MusicRatingRequest;
import com.musa.music.entity.MusicContentType;
import com.musa.music.entity.MusicFavorite;
import com.musa.music.entity.MusicRating;
import com.musa.music.service.MusicFavoriteService;
import com.musa.music.service.MusicRatingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/music")
public class MusicUserActionsController {

    private final MusicRatingService musicRatingService;
    private final MusicFavoriteService musicFavoriteService;

    public MusicUserActionsController(
            MusicRatingService musicRatingService,
            MusicFavoriteService musicFavoriteService
    ) {
        this.musicRatingService = musicRatingService;
        this.musicFavoriteService = musicFavoriteService;
    }

    @PostMapping("/ratings")
    public ResponseEntity<MusicRating> saveRating(
            @Valid @RequestBody MusicRatingRequest request
    ) {
        return ResponseEntity.ok(
                musicRatingService.saveOrUpdateRating(request)
        );
    }

    @GetMapping("/ratings/user/{userId}")
    public ResponseEntity<Double> getUserRating(
            @PathVariable Long userId,
            @RequestParam String spotifyId,
            @RequestParam MusicContentType contentType
    ) {
        return ResponseEntity.ok(
                musicRatingService.getUserRating(
                        userId,
                        spotifyId,
                        contentType
                )
        );
    }

    @PostMapping("/favorites")
    public ResponseEntity<MusicFavorite> addFavorite(
            @Valid @RequestBody MusicFavoriteRequest request
    ) {
        return ResponseEntity.ok(
                musicFavoriteService.addFavorite(request)
        );
    }

    @DeleteMapping("/favorites/{musicContentId}")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable Long musicContentId,
            @RequestParam Long userId
    ) {
        musicFavoriteService.removeFavorite(
                userId,
                musicContentId
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/favorites/user/{userId}")
        public ResponseEntity<Boolean> isFavorite(
                @PathVariable Long userId,
                @RequestParam String spotifyId,
                @RequestParam MusicContentType contentType
        ) {
        return ResponseEntity.ok(
                musicFavoriteService.isFavorite(
                        userId,
                        spotifyId,
                        contentType
                )
        );
    }
}