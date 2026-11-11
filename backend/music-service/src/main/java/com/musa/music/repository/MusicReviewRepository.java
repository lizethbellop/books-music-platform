package com.musa.music.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.musa.music.entity.MusicReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MusicReviewRepository
        extends JpaRepository<MusicReview, Long> {

    Optional<MusicReview> findByUserIdAndMusicContentId(
            UUID userId,
            Long musicContentId
    );

    List<MusicReview> findByMusicContentId(
            Long musicContentId
    );

    List<MusicReview> findByUserIdOrderByCreatedAtDesc(
            UUID userId
    );

    long countByUserId(UUID userId);
}