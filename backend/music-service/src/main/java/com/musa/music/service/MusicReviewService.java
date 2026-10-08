package com.musa.music.service;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.musa.music.dto.MusicReviewRequest;
import com.musa.music.dto.MusicReviewUpdateRequest;
import com.musa.music.entity.MusicContent;
import com.musa.music.entity.MusicContentType;
import com.musa.music.entity.MusicReview;
import com.musa.music.repository.MusicReviewRepository;
import org.springframework.stereotype.Service;
import com.musa.music.dto.ReviewedMusicResponse;
import com.musa.music.repository.MusicRatingRepository;

import java.util.List;

@Service
public class MusicReviewService {

    private final MusicReviewRepository musicReviewRepository;
    private final MusicContentService musicContentService;
    private final MusicRatingRepository musicRatingRepository;

    public MusicReviewService(
                MusicReviewRepository musicReviewRepository,
                MusicContentService musicContentService,
                MusicRatingRepository musicRatingRepository
    ) {
        this.musicReviewRepository = musicReviewRepository;
        this.musicContentService = musicContentService;
        this.musicRatingRepository = musicRatingRepository;
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
            UUID userId,
            MusicReviewUpdateRequest request
    ) {

        MusicReview review = requireOwnedReview(reviewId, userId);

        review.setReviewText(request.reviewText());

        return musicReviewRepository.save(review);
    }

    public void deleteReview(Long reviewId, UUID userId) {
        MusicReview review = requireOwnedReview(reviewId, userId);
        musicReviewRepository.delete(review);
    }

    private MusicReview requireOwnedReview(Long reviewId, UUID userId) {
        MusicReview review = musicReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Reseña no encontrada"
                ));
        if (!userId.equals(review.getUserId())) {
            throw new AccessDeniedException("Solo puedes modificar tus propias reseñas.");
        }
        return review;
    }

    public List<MusicReview> getReviewsByMusicContentId(
            Long musicContentId
    ) {
        return musicReviewRepository
                .findByMusicContentId(musicContentId);
    }

    public MusicReview getUserReview(
            UUID userId,
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
            UUID userId,
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

     public List<ReviewedMusicResponse> getReviewedMusicByUser(
                UUID userId
     ) {
        return musicReviewRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(review -> {

                        MusicContent content = review.getMusicContent();

                        Double rating = musicRatingRepository
                                .findByUserIdAndMusicContentId(
                                        userId,
                                        content.getId()
                                )
                                .map(musicRating -> musicRating.getRating())
                                .orElse(0.0);

                        return new ReviewedMusicResponse(
                                content.getSpotifyId(),
                                content.getContentType(),
                                content.getName(),
                                content.getArtistName(),
                                content.getImageUrl(),
                                rating,
                                review.getCreatedAt()
                        );
                })
                .toList();
     }
}