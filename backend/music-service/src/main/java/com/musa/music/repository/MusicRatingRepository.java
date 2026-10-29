package com.musa.music.repository;

import java.util.Optional;
import java.util.UUID;

import com.musa.music.entity.MusicRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MusicRatingRepository
        extends JpaRepository<MusicRating, Long> {

    Optional<MusicRating> findByUserIdAndMusicContentId(
            UUID userId,
            Long musicContentId
    );

    long countByUserId(UUID userId);

    @Query("""
            SELECT AVG(r.rating)
            FROM MusicRating r
            WHERE r.userId = :userId
            """)
    Double findAverageRatingByUserId(
            @Param("userId") UUID userId
    );

    @Query("""
            SELECT AVG(r.rating)
            FROM MusicRating r
            """)
    Double findGlobalAverageRating();
}