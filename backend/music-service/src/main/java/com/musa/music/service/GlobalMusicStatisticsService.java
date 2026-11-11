package com.musa.music.service;

import com.musa.music.dto.GlobalMusicStatisticsResponse;
import com.musa.music.repository.MusicFavoriteRepository;
import com.musa.music.repository.MusicRatingRepository;
import com.musa.music.repository.MusicReviewRepository;
import org.springframework.stereotype.Service;

@Service
public class GlobalMusicStatisticsService {

    private final MusicReviewRepository musicReviewRepository;
    private final MusicRatingRepository musicRatingRepository;
    private final MusicFavoriteRepository musicFavoriteRepository;

    public GlobalMusicStatisticsService(
            MusicReviewRepository musicReviewRepository,
            MusicRatingRepository musicRatingRepository,
            MusicFavoriteRepository musicFavoriteRepository
    ) {
        this.musicReviewRepository = musicReviewRepository;
        this.musicRatingRepository = musicRatingRepository;
        this.musicFavoriteRepository = musicFavoriteRepository;
    }

    public GlobalMusicStatisticsResponse getGlobalStatistics() {

        long reviews = musicReviewRepository.count();

        long ratings = musicRatingRepository.count();

        long favorites = musicFavoriteRepository.count();

        Double averageRating =
                musicRatingRepository.findGlobalAverageRating();

        return new GlobalMusicStatisticsResponse(
                reviews,
                ratings,
                favorites,
                averageRating
        );
    }
}