package com.musa.music.service;

import com.musa.music.dto.MusicSearchItemResponse;
import com.musa.music.entity.MusicContent;
import com.musa.music.repository.MusicContentRepository;
import com.musa.music.spotify.SpotifySearchService;
import org.springframework.stereotype.Service;
import com.musa.music.spotify.SpotifyAuthService;

import java.util.List;

@Service
public class MusicSearchService {

    private final MusicContentRepository musicContentRepository;
    private final SpotifySearchService spotifySearchService;
    private final SpotifyAuthService spotifyAuthService;

    public MusicSearchService(
            MusicContentRepository musicContentRepository,
            SpotifySearchService spotifySearchService,
            SpotifyAuthService spotifyAuthService
    ) {
        this.musicContentRepository = musicContentRepository;
        this.spotifySearchService = spotifySearchService;
        this.spotifyAuthService = spotifyAuthService;
    }

    public List<MusicSearchItemResponse> search(String query) {
        List<MusicContent> localResults =
            musicContentRepository.searchLocal(query);

        if (!localResults.isEmpty()) {
            return localResults.stream()
                    .map(this::toResponse)
                    .toList();
        }

        if (!spotifyAuthService.isConfigured()) {
            return List.of();
        }

        List<MusicSearchItemResponse> spotifyResults =
                spotifySearchService.search(query);

        saveSpotifyResults(spotifyResults);

        return spotifyResults;
    }

    private MusicSearchItemResponse toResponse(
            MusicContent content
    ) {
        return new MusicSearchItemResponse(
                content.getSpotifyId(),
                content.getContentType(),
                content.getName(),
                content.getArtistName(),
                content.getImageUrl()
        );
    }

    private void saveSpotifyResults(
            List<MusicSearchItemResponse> results
    ) {

        for (MusicSearchItemResponse result : results) {

            boolean alreadyExists =
                    musicContentRepository
                            .findBySpotifyIdAndContentType(
                                    result.spotifyId(),
                                    result.contentType()
                            )
                            .isPresent();

            if (alreadyExists) {
                continue;
            }

            MusicContent content = MusicContent.builder()
                    .spotifyId(result.spotifyId())
                    .contentType(result.contentType())
                    .name(result.name())
                    .artistName(result.artistName())
                    .imageUrl(result.imageUrl())
                    .build();

            musicContentRepository.save(content);
        }
    }
}