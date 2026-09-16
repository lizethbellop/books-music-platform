package com.musa.music.repository;

import com.musa.music.entity.MusicFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MusicFavoriteRepository
        extends JpaRepository<MusicFavorite, Long> {

    Optional<MusicFavorite> findByUserIdAndMusicContentId(
            Long userId,
            Long musicContentId
    );

    boolean existsByUserIdAndMusicContentId(
            Long userId,
            Long musicContentId
    );
}