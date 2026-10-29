package com.musa.music.repository;

import java.util.Optional;
import java.util.UUID;

import com.musa.music.entity.MusicFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MusicFavoriteRepository
        extends JpaRepository<MusicFavorite, Long> {

    Optional<MusicFavorite> findByUserIdAndMusicContentId(
            UUID userId,
            Long musicContentId
    );

    boolean existsByUserIdAndMusicContentId(
            UUID userId,
            Long musicContentId
    );

    long countByUserId(UUID userId);
}