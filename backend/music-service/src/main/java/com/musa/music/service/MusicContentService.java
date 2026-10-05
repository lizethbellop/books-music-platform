package com.musa.music.service;

import com.musa.music.entity.MusicContent;
import com.musa.music.entity.MusicContentType;
import com.musa.music.repository.MusicContentRepository;
import org.springframework.stereotype.Service;

@Service
public class MusicContentService {

    private final MusicContentRepository musicContentRepository;

    public MusicContentService(
            MusicContentRepository musicContentRepository
    ) {
        this.musicContentRepository = musicContentRepository;
    }

    public MusicContent findOrCreate(
            String spotifyId,
            MusicContentType contentType,
            String name,
            String artistName,
            String imageUrl
    ) {
        return musicContentRepository
                .findBySpotifyIdAndContentType(
                        spotifyId,
                        contentType
                )
                .orElseGet(() -> {
                    MusicContent content = MusicContent.builder()
                            .spotifyId(spotifyId)
                            .contentType(contentType)
                            .name(name)
                            .artistName(artistName)
                            .imageUrl(imageUrl)
                            .build();

                    return musicContentRepository.save(content);
                });
    }

    public MusicContent findExisting(
            String spotifyId,
            MusicContentType contentType
    ) {
        return musicContentRepository
                .findBySpotifyIdAndContentType(
                        spotifyId,
                        contentType
                )
                .orElse(null);
        }
}