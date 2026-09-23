package com.musa.music.repository;

import com.musa.music.entity.MusicReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MusicReviewRepository extends JpaRepository<MusicReview, Long> {

    Optional<MusicReview> findByUserIdAndMusicContentId(
            Long userId,
            Long musicContentId
    );

    List<MusicReview> findByMusicContentId(
            Long musicContentId
    );
}