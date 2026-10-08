package com.musa.profile.dto.catalog;

public record MusicCatalogResponse(
        String spotifyId,
        String contentType,
        String name,
        String artistName,
        String imageUrl,
        String spotifyUrl,
        String releaseDate,
        Integer totalTracks
) {
}