package com.musa.books.service;

import com.musa.books.dto.BookSearchResultDto;
import com.musa.books.dto.OpenLibraryBookDto;
import com.musa.books.dto.OpenLibrarySearchResponse;
import com.musa.books.dto.OpenLibraryEditionDto;
import com.musa.books.dto.OpenLibraryEditionsResponse;
import com.musa.books.dto.BookDetailDto;
import com.musa.books.dto.OpenLibraryWorkDto;
import tools.jackson.databind.JsonNode;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class OpenLibraryService {

    private final RestClient restClient;

    public OpenLibraryService(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<BookSearchResultDto> searchBooks(String query) {

        OpenLibrarySearchResponse response = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search.json")
                        .queryParam("q", query)
                        .queryParam("limit", 20)
                        .build()
                )
                .retrieve()
                .body(OpenLibrarySearchResponse.class);

        if (response == null || response.getDocs() == null) {
            return Collections.emptyList();
        }

        return response.getDocs()
                .stream()
                .map(this::toSearchResult)
                .toList();
    }

    public List<BookSearchResultDto> getExploreBooks() {

        OpenLibrarySearchResponse response = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search.json")
                        .queryParam(
                                "q",
                                "subject_key:fiction"
                        )
                        .queryParam(
                                "sort",
                                "editions"
                        )
                        .queryParam(
                                "limit",
                                12
                        )
                        .build()
                )
                .retrieve()
                .body(OpenLibrarySearchResponse.class);

        if (response == null
                || response.getDocs() == null) {

            return Collections.emptyList();
        }

        return response.getDocs()
                .stream()
                .map(this::toSearchResult)
                .toList();
    }

    private BookSearchResultDto toSearchResult(OpenLibraryBookDto book) {

        String externalId = null;

        if (book.getKey() != null) {
            externalId = book.getKey()
                    .replace("/works/", "");
        }

        String coverUrl = buildCoverUrl(book.getCoverId());

        if (coverUrl == null && externalId != null) {
            coverUrl = findCoverFromEditions(externalId);
        }

        return new BookSearchResultDto(
                externalId,
                book.getTitle(),
                book.getAuthorNames(),
                book.getFirstPublishYear(),
                coverUrl
        );
    }

    private String findCoverFromEditions(
            String externalId,
            String size
    ) {

        OpenLibraryEditionsResponse response = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/works/{workId}/editions.json")
                        .queryParam("limit", 10)
                        .build(externalId)
                )
                .retrieve()
                .body(OpenLibraryEditionsResponse.class);

        if (response == null || response.getEntries() == null) {
            return null;
        }

        for (OpenLibraryEditionDto edition : response.getEntries()) {

            if (edition.getCovers() != null
                    && !edition.getCovers().isEmpty()) {

                Integer coverId =
                        edition.getCovers().get(0);

                return buildCoverUrl(
                        coverId,
                        size
                );
            }
        }

        return null;
    }

        private String findCoverFromEditions(
            String externalId
    ) {
        return findCoverFromEditions(
                externalId,
                "M"
        );
    }

    private String buildCoverUrl(Integer coverId) {
        return buildCoverUrl(coverId, "M");
    }

    private String buildCoverUrl(
            Integer coverId,
            String size
    ) {

        if (coverId == null) {
            return null;
        }

        return "https://covers.openlibrary.org/b/id/"
                + coverId
                + "-"
                + size
                + ".jpg";
    }

    private String extractDescription(OpenLibraryWorkDto work) {

        JsonNode description = work.getDescription();

        if (description == null) {
            return null;
        }

        if (description.isTextual()) {
            return description.asText();
        }

        if (description.get("value") != null) {
            return description
                    .get("value")
                    .asText();
        }

        return null;
    }

    public BookDetailDto getBookDetail(String externalId) {

        OpenLibraryWorkDto work = restClient
                .get()
                .uri("/works/{workId}.json", externalId)
                .retrieve()
                .body(OpenLibraryWorkDto.class);

        if (work == null) {
            throw new IllegalArgumentException(
                    "No se pudo obtener la información del libro"
            );
        }

        List<String> authors = getAuthorNames(work);

        String description = extractDescription(work);

        String coverUrl = null;

        if (work.getCovers() != null && !work.getCovers().isEmpty()) {
            coverUrl = buildCoverUrl(
                    work.getCovers().get(0),
                    "L"
            );
        }

        if (coverUrl == null) {
            coverUrl = findCoverFromEditions(
                    externalId,
                    "L"
            );
        }

        return new BookDetailDto(
                externalId,
                work.getTitle(),
                authors,
                description,
                work.getFirstPublishDate(),
                work.getSubjects(),
                coverUrl
        );
    }

    private List<String> getAuthorNames(OpenLibraryWorkDto work) {

        List<String> authorNames = new ArrayList<>();

        JsonNode authorsNode = work.getAuthors();

        if (authorsNode == null || !authorsNode.isArray()) {
            return authorNames;
        }

        for (JsonNode authorEntry : authorsNode) {

            JsonNode authorNode = authorEntry.get("author");

            if (authorNode == null) {
                continue;
            }

            JsonNode keyNode = authorNode.get("key");

            if (keyNode == null) {
                continue;
            }

            String authorKey = keyNode.asText()
                    .replace("/authors/", "");

            JsonNode authorResponse = restClient
                    .get()
                    .uri("/authors/{authorId}.json", authorKey)
                    .retrieve()
                    .body(JsonNode.class);

            if (authorResponse != null
                    && authorResponse.get("name") != null) {

                authorNames.add(
                        authorResponse.get("name").asText()
                );
            }
        }

        return authorNames;
    }

}