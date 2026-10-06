package com.musa.music.repository;

import com.musa.music.entity.MusicContent;
import com.musa.music.entity.MusicContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MusicContentRepository
        extends JpaRepository<MusicContent, Long> {

    Optional<MusicContent> findBySpotifyIdAndContentType(
            String spotifyId,
            MusicContentType contentType
    );

    @Query(
        value = """
            SELECT *
            FROM music_content
            WHERE TRANSLATE(
                    LOWER(name),
                    'áéíóúüñ',
                    'aeiouun'
                  )
                  LIKE CONCAT(
                    '%',
                    TRANSLATE(
                        LOWER(:query),
                        'áéíóúüñ',
                        'aeiouun'
                    ),
                    '%'
                  )
               OR TRANSLATE(
                    LOWER(COALESCE(artist_name, '')),
                    'áéíóúüñ',
                    'aeiouun'
                  )
                  LIKE CONCAT(
                    '%',
                    TRANSLATE(
                        LOWER(:query),
                        'áéíóúüñ',
                        'aeiouun'
                    ),
                    '%'
                  )
            """,
        nativeQuery = true
    )
    List<MusicContent> searchLocal(
            @Param("query") String query
    );
}