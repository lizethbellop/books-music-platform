package com.musa.music.service;

import com.musa.music.dto.MusicRatingRequest;
import com.musa.music.entity.MusicContent;
import com.musa.music.entity.MusicRating;
import com.musa.music.repository.MusicRatingRepository;
import org.springframework.stereotype.Service;

@Service
public class MusicRatingService {

    private final MusicRatingRepository musicRatingRepository;
    private final MusicContentService musicContentService;

    public MusicRatingService(
            MusicRatingRepository musicRatingRepository,
            MusicContentService musicContentService
    ) {
        this.musicRatingRepository = musicRatingRepository;
        this.musicContentService = musicContentService;
    }

    public MusicRating saveOrUpdateRating(
            MusicRatingRequest request
    ) {
        MusicContent content =
                musicContentService.findOrCreate(
                        request.spotifyId(),
                        request.contentType(),
                        request.name(),
                        request.artistName(),
                        request.imageUrl()
                );

        MusicRating rating = musicRatingRepository
                .findByUserIdAndMusicContentId(
                        request.userId(),
                        content.getId()
                )
                .orElse(
                        MusicRating.builder()
                                .userId(request.userId())
                                .musicContent(content)
                                .build()
                );

        rating.setRating(request.rating());

        return musicRatingRepository.save(rating);
    }
}