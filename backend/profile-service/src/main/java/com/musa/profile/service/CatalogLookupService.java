package com.musa.profile.service;

import java.util.Objects;

import com.musa.profile.dto.catalog.BookCatalogResponse;
import com.musa.profile.dto.catalog.CatalogResolution;
import com.musa.profile.dto.catalog.MusicCatalogResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class CatalogLookupService {

    private final RestClient booksClient;
    private final RestClient musicClient;

    public CatalogLookupService(
            @Qualifier("booksCatalogRestClient") RestClient booksClient,
            @Qualifier("musicCatalogRestClient") RestClient musicClient
    ) {
        this.booksClient = booksClient;
        this.musicClient = musicClient;
    }

    public CatalogResolution resolve(
            String elementType,
            String referenceId,
            String accessToken
    ) {
        if (elementType == null
                || referenceId == null
                || referenceId.isBlank()) {
            return CatalogResolution.unavailable();
        }

        try {
            return switch (elementType) {
                case "BOOK" -> resolveBook(referenceId, accessToken);
                case "SONG", "ARTIST" ->
                        resolveMusic(elementType, referenceId, accessToken);
                default -> CatalogResolution.unavailable();
            };
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return CatalogResolution.notFound();
            }

            return CatalogResolution.unavailable();
        } catch (RestClientException exception) {
            return CatalogResolution.unavailable();
        }
    }

    private CatalogResolution resolveBook(
            String referenceId,
            String accessToken
    ) {
        BookCatalogResponse content = booksClient
                .get()
                .uri("/books/{externalId}", referenceId)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(BookCatalogResponse.class);

        if (content == null
                || !Objects.equals(referenceId, content.externalId())
                || content.title() == null
                || content.title().isBlank()) {
            return CatalogResolution.unavailable();
        }

        return CatalogResolution.available(content);
    }

    private CatalogResolution resolveMusic(
            String elementType,
            String referenceId,
            String accessToken
    ) {
        MusicCatalogResponse content = musicClient
                .get()
                .uri(
                        "/api/music/{type}/{spotifyId}",
                        elementType,
                        referenceId
                )
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(MusicCatalogResponse.class);

        if (content == null
                || !Objects.equals(referenceId, content.spotifyId())
                || !Objects.equals(elementType, content.contentType())
                || content.name() == null
                || content.name().isBlank()) {
            return CatalogResolution.unavailable();
        }

        return CatalogResolution.available(content);
    }
}