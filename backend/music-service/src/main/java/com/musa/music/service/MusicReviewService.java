package com.musa.music.service;

import com.musa.music.dto.MusicReviewRequest;
import com.musa.music.dto.MusicReviewUpdateRequest;
import com.musa.music.entity.MusicContent;
import com.musa.music.entity.MusicContentType;
import com.musa.music.entity.MusicReview;
import com.musa.music.repository.MusicReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MusicReviewService {

    private final MusicReviewRepository musicReviewRepository;
    private final MusicContentService musicContentService;

    public MusicReviewService(
            MusicReviewRepository musicReviewRepository,
            MusicContentService musicContentService
    ) {
        this.musicReviewRepository = musicReviewRepository;
        this.musicContentService = musicContentService;
    }

    public MusicReview createReview(MusicReviewRequest request) {

        MusicContent content = musicContentService.findOrCreate(
                request.spotifyId(),
                request.contentType(),
                request.name(),
                request.artistName(),
                request.imageUrl()
        );

        return musicReviewRepository
                .findByUserIdAndMusicContentId(
                        request.userId(),
                        content.getId()
                )
                .orElseGet(() -> {
                    MusicReview review = MusicReview.builder()
                            .userId(request.userId())
                            .musicContent(content)
                            .reviewText(request.reviewText())
                            .build();

                    return musicReviewRepository.save(review);
                });
    }

    public MusicReview updateReview(
            Long reviewId,
            MusicReviewUpdateRequest request
    ) {

        MusicReview review = musicReviewRepository
                .findById(reviewId)
                .orElseThrow(
                        () -> new RuntimeException("Reseña no encontrada")
                );

        review.setReviewText(request.reviewText());

        return musicReviewRepository.save(review);
    }

    public void deleteReview(Long reviewId) {

        if (!musicReviewRepository.existsById(reviewId)) {
            throw new RuntimeException("Reseña no encontrada");
        }

        musicReviewRepository.deleteById(reviewId);
    }

    public List<MusicReview> getReviewsByMusicContentId(
            Long musicContentId
    ) {
        return musicReviewRepository
                .findByMusicContentId(musicContentId);
    }

    public MusicReview getUserReview(
            Long userId,
            Long musicContentId
    ) {
        return musicReviewRepository
                .findByUserIdAndMusicContentId(
                        userId,
                        musicContentId
                )
                .orElse(null);
    }

    public MusicReview getUserReviewBySpotify(
            Long userId,
            String spotifyId,
            MusicContentType contentType
    ) {
        MusicContent content = musicContentService
                .findExisting(
                        spotifyId,
                        contentType
                );

        if (content == null) {
            return null;
        }

        return musicReviewRepository
                .findByUserIdAndMusicContentId(
                        userId,
                        content.getId()
                )
                .orElse(null);
    }

    public List<MusicReview> getReviewsBySpotify(
                String spotifyId,
                MusicContentType contentType
        ) {
        MusicContent content = musicContentService
                .findExisting(
                        spotifyId,
                        contentType
                );

        if (content == null) {
                return List.of();
        }

        return musicReviewRepository
                .findByMusicContentId(
                        content.getId()
                );
        }
}