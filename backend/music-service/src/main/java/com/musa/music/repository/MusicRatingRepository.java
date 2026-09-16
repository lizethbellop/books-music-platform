package com.musa.music.repository;

import com.musa.music.entity.MusicRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MusicRatingRepository
        extends JpaRepository<MusicRating, Long> {

    Optional<MusicRating> findByUserIdAndMusicContentId(
            Long userId,
            Long musicContentId
    );
}