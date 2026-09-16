package com.musa.music.service;

import com.musa.music.dto.MusicFavoriteRequest;
import com.musa.music.entity.MusicContent;
import com.musa.music.entity.MusicFavorite;
import com.musa.music.repository.MusicFavoriteRepository;
import org.springframework.stereotype.Service;

@Service
public class MusicFavoriteService {

    private final MusicFavoriteRepository musicFavoriteRepository;
    private final MusicContentService musicContentService;

    public MusicFavoriteService(
            MusicFavoriteRepository musicFavoriteRepository,
            MusicContentService musicContentService
    ) {
        this.musicFavoriteRepository =
                musicFavoriteRepository;
        this.musicContentService = musicContentService;
    }

    public MusicFavorite addFavorite(
            MusicFavoriteRequest request
    ) {
        MusicContent content =
                musicContentService.findOrCreate(
                        request.spotifyId(),
                        request.contentType(),
                        request.name(),
                        request.artistName(),
                        request.imageUrl()
                );

        return musicFavoriteRepository
                .findByUserIdAndMusicContentId(
                        request.userId(),
                        content.getId()
                )
                .orElseGet(() -> {
                    MusicFavorite favorite =
                            MusicFavorite.builder()
                                    .userId(request.userId())
                                    .musicContent(content)
                                    .build();

                    return musicFavoriteRepository.save(
                            favorite
                    );
                });
    }

    public void removeFavorite(
            Long userId,
            Long musicContentId
    ) {
        MusicFavorite favorite =
                musicFavoriteRepository
                        .findByUserIdAndMusicContentId(
                                userId,
                                musicContentId
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Favorite not found"
                                )
                        );

        musicFavoriteRepository.delete(favorite);
    }
}