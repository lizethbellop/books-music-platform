package com.musa.music.controller;

import com.musa.music.dto.MusicReviewRequest;
import com.musa.music.dto.MusicReviewUpdateRequest;
import com.musa.music.entity.MusicReview;
import com.musa.music.service.MusicReviewService;
import com.musa.music.entity.MusicContentType;
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
            @Valid @RequestBody MusicReviewRequest request
    ) {
        return musicReviewService.createReview(request);
    }

    @PutMapping("/{reviewId}")
    public MusicReview updateReview(
            @PathVariable Long reviewId,
            @RequestBody MusicReviewUpdateRequest request
    ) {
        return musicReviewService.updateReview(
                reviewId,
                request
        );
    }

    @DeleteMapping("/{reviewId}")
    public void deleteReview(
            @PathVariable Long reviewId
    ) {
        musicReviewService.deleteReview(reviewId);
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
                @PathVariable Long userId,
                @PathVariable Long musicContentId
        ) {
            return musicReviewService.getUserReview(
                    userId,
                    musicContentId
            );
        }

        @GetMapping("/user/{userId}")
    public MusicReview getUserReviewBySpotify(
            @PathVariable Long userId,
            @RequestParam String spotifyId,
            @RequestParam MusicContentType contentType
    ) {
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
}