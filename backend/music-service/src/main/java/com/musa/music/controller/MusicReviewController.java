package com.musa.music.controller;

import java.util.UUID;
import com.musa.music.config.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import com.musa.music.dto.MusicReviewRequest;
import com.musa.music.dto.MusicReviewUpdateRequest;
import com.musa.music.dto.ReviewedMusicResponse;
import com.musa.music.entity.MusicContentType;
import com.musa.music.entity.MusicReview;
import com.musa.music.service.MusicReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/music/reviews")
public class MusicReviewController {

    private final MusicReviewService musicReviewService;

    public MusicReviewController(
            MusicReviewService musicReviewService
    ) {
        this.musicReviewService = musicReviewService;
    }

    @PostMapping
    public MusicReview createReview(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MusicReviewRequest request
    ) {
        AuthenticatedUser.requireOwn(jwt, request.userId());

        return musicReviewService.createReview(request);
    }

    @PutMapping("/{reviewId}")
    public MusicReview updateReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reviewId,
            @RequestBody MusicReviewUpdateRequest request
    ) {
        return musicReviewService.updateReview(
                reviewId,
                UUID.fromString(jwt.getSubject()),
                request
        );
    }

    @DeleteMapping("/{reviewId}")
    public void deleteReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reviewId
    ) {
        musicReviewService.deleteReview(reviewId, UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/content/{musicContentId}")
    public List<MusicReview> getReviews(
            @PathVariable Long musicContentId
    ) {
        return musicReviewService
                .getReviewsByMusicContentId(musicContentId);
    }

    @GetMapping("/user/{userId}/content/{musicContentId}")
    public MusicReview getUserReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("userId") UUID suppliedUserId,
            @PathVariable Long musicContentId
    ) {
        UUID userId = AuthenticatedUser.requireOwn(jwt, suppliedUserId);

        return musicReviewService.getUserReview(
                userId,
                musicContentId
        );
    }

    @GetMapping("/user/{userId}")
    public MusicReview getUserReviewBySpotify(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("userId") UUID suppliedUserId,
            @RequestParam String spotifyId,
            @RequestParam MusicContentType contentType
    ) {
        UUID userId = AuthenticatedUser.requireOwn(jwt, suppliedUserId);

        return musicReviewService.getUserReviewBySpotify(
                userId,
                spotifyId,
                contentType
        );
    }

    @GetMapping("/content")
    public List<MusicReview> getReviewsBySpotify(
            @RequestParam String spotifyId,
            @RequestParam MusicContentType contentType
    ) {
        return musicReviewService.getReviewsBySpotify(
                spotifyId,
                contentType
        );
    }

    @GetMapping("/user/{userId}/reviewed")
    public List<ReviewedMusicResponse> getReviewedMusic(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("userId") UUID suppliedUserId
    ) {
        UUID userId = AuthenticatedUser.requireOwn(jwt, suppliedUserId);

        return musicReviewService.getReviewedMusicByUser(userId);
    }
}