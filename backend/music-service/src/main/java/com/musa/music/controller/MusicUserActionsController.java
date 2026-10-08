package com.musa.music.controller;

import java.util.UUID;
import com.musa.music.config.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

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
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MusicRatingRequest request
    ) {
        AuthenticatedUser.requireOwn(jwt, request.userId());

        return ResponseEntity.ok(
                musicRatingService.saveOrUpdateRating(request)
        );
    }

    @GetMapping("/ratings/user/{userId}")
    public ResponseEntity<Double> getUserRating(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("userId") UUID suppliedUserId,
            @RequestParam String spotifyId,
            @RequestParam MusicContentType contentType
    ) {
        UUID userId = AuthenticatedUser.requireOwn(jwt, suppliedUserId);

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
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MusicFavoriteRequest request
    ) {
        AuthenticatedUser.requireOwn(jwt, request.userId());

        return ResponseEntity.ok(
                musicFavoriteService.addFavorite(request)
        );
    }

    @DeleteMapping("/favorites")
        public ResponseEntity<Void> removeFavorite(
                @AuthenticationPrincipal Jwt jwt,
                @RequestParam(name = "userId", required = false) UUID suppliedUserId,
                @RequestParam String spotifyId,
                @RequestParam MusicContentType contentType
        ) {
        UUID userId = AuthenticatedUser.requireOwn(jwt, suppliedUserId);

        musicFavoriteService.removeFavorite(
                userId,
                spotifyId,
                contentType
        );

        return ResponseEntity.noContent().build();
        }

    @GetMapping("/favorites/user/{userId}")
        public ResponseEntity<Boolean> isFavorite(
                @AuthenticationPrincipal Jwt jwt,
            @PathVariable("userId") UUID suppliedUserId,
                @RequestParam String spotifyId,
                @RequestParam MusicContentType contentType
        ) {
        UUID userId = AuthenticatedUser.requireOwn(jwt, suppliedUserId);

        return ResponseEntity.ok(
                musicFavoriteService.isFavorite(
                        userId,
                        spotifyId,
                        contentType
                )
        );
    }
}