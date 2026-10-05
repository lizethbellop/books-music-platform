package com.musa.music.service;

import com.musa.music.dto.MusicDetailResponse;
import com.musa.music.entity.MusicContent;
import com.musa.music.entity.MusicContentType;
import com.musa.music.repository.MusicContentRepository;
import com.musa.music.spotify.SpotifyAuthService;
import com.musa.music.spotify.SpotifyDetailService;
import org.springframework.stereotype.Service;

@Service
public class MusicDetailService {

    private final MusicContentRepository musicContentRepository;
    private final SpotifyDetailService spotifyDetailService;
    private final SpotifyAuthService spotifyAuthService;

    public MusicDetailService(
            MusicContentRepository musicContentRepository,
            SpotifyDetailService spotifyDetailService,
            SpotifyAuthService spotifyAuthService
    ) {
        this.musicContentRepository = musicContentRepository;
        this.spotifyDetailService = spotifyDetailService;
        this.spotifyAuthService = spotifyAuthService;
    }

    public MusicDetailResponse getDetail(
            MusicContentType contentType,
            String spotifyId
    ) {

        MusicContent localContent = musicContentRepository
                .findBySpotifyIdAndContentType(
                        spotifyId,
                        contentType
                )
                .orElse(null);

        if (localContent != null) {
            return toResponse(localContent);
        }

        if (!spotifyAuthService.isConfigured()) {
            throw new RuntimeException(
                    "El contenido no está disponible localmente y Spotify no está configurado"
            );
        }

        MusicDetailResponse spotifyDetail =
                spotifyDetailService.getDetail(
                        contentType,
                        spotifyId
                );

        saveSpotifyDetail(spotifyDetail);

        return spotifyDetail;
    }

    private MusicDetailResponse toResponse(
            MusicContent content
    ) {
        return new MusicDetailResponse(
                content.getSpotifyId(),
                content.getContentType(),
                content.getName(),
                content.getArtistName(),
                content.getImageUrl(),
                null,
                null,
                null
        );
    }

    private void saveSpotifyDetail(
            MusicDetailResponse detail
    ) {

        boolean alreadyExists =
                musicContentRepository
                        .findBySpotifyIdAndContentType(
                                detail.spotifyId(),
                                detail.contentType()
                        )
                        .isPresent();

        if (alreadyExists) {
            return;
        }

        MusicContent content = MusicContent.builder()
                .spotifyId(detail.spotifyId())
                .contentType(detail.contentType())
                .name(detail.name())
                .artistName(detail.artistName())
                .imageUrl(detail.imageUrl())
                .build();

        musicContentRepository.save(content);
    }
}