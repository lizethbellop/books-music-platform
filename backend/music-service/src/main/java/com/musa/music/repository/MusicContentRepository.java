package com.musa.music.repository;

import com.musa.music.entity.MusicContent;
import com.musa.music.entity.MusicContentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MusicContentRepository
        extends JpaRepository<MusicContent, Long> {

    Optional<MusicContent> findBySpotifyIdAndContentType(
            String spotifyId,
            MusicContentType contentType
    );
}