package com.musa.music.service;

import java.util.UUID;

import com.musa.music.dto.MusicStatisticsResponse;
import com.musa.music.repository.MusicFavoriteRepository;
import com.musa.music.repository.MusicRatingRepository;
import com.musa.music.repository.MusicReviewRepository;
import org.springframework.stereotype.Service;

@Service
public class MusicStatisticsService {

    private final MusicReviewRepository musicReviewRepository;
    private final MusicRatingRepository musicRatingRepository;
    private final MusicFavoriteRepository musicFavoriteRepository;

    public MusicStatisticsService(
            MusicReviewRepository musicReviewRepository,
            MusicRatingRepository musicRatingRepository,
            MusicFavoriteRepository musicFavoriteRepository
    ) {
        this.musicReviewRepository = musicReviewRepository;
        this.musicRatingRepository = musicRatingRepository;
        this.musicFavoriteRepository = musicFavoriteRepository;
    }

    public MusicStatisticsResponse getUserStatistics(
            UUID userId
    ) {
        long reviews =
                musicReviewRepository.countByUserId(userId);

        long ratings =
                musicRatingRepository.countByUserId(userId);

        long favorites =
                musicFavoriteRepository.countByUserId(userId);

        Double averageRating =
                musicRatingRepository
                        .findAverageRatingByUserId(userId);

        return new MusicStatisticsResponse(
                reviews,
                ratings,
                favorites,
                averageRating
        );
    }
}