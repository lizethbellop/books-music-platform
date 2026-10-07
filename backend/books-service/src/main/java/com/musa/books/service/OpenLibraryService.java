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
    private final TranslationService translationService;

    public OpenLibraryService(
            RestClient restClient,
            TranslationService translationService
    ) {
        this.restClient = restClient;
        this.translationService = translationService;
    }

    public List<BookSearchResultDto> searchBooks(String query) {

        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        String cleanQuery = query.trim();

        /*
        * 1. Primero intentamos la búsqueda tal como
        * la escribió el usuario.
        */
        List<BookSearchResultDto> results =
                searchOpenLibrary(cleanQuery);

        if (!results.isEmpty()) {
            return results;
        }

        /*
        * 2. Si Open Library no encontró nada,
        * traducimos la consulta al inglés.
        */
        try {

            String englishQuery =
                    translationService
                            .translateToEnglish(cleanQuery);

            /*
            * Evitamos repetir exactamente
            * la misma búsqueda.
            */
            if (englishQuery == null
                    || englishQuery.isBlank()
                    || englishQuery.equalsIgnoreCase(
                            cleanQuery
                    )) {

                return results;
            }

            return searchOpenLibrary(
                    englishQuery.trim()
            );

        } catch (Exception e) {

            /*
            * Si DeepL falla, la búsqueda original
            * sigue siendo válida.
            */
            return results;
        }
    }

    private List<BookSearchResultDto> searchOpenLibrary(
            String query
    ) {

        OpenLibrarySearchResponse response =
                restClient
                        .get()
                        .uri(uriBuilder ->
                                uriBuilder
                                        .path("/search.json")
                                        .queryParam(
                                                "q",
                                                query
                                        )
                                        .queryParam(
                                                "lang",
                                                "es"
                                        )
                                        .queryParam(
                                                "limit",
                                                20
                                        )
                                        .build()
                        )
                        .retrieve()
                        .body(
                                OpenLibrarySearchResponse.class
                        );

        if (response == null
                || response.getDocs() == null) {

            return Collections.emptyList();
        }

        return response
                .getDocs()
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

    if (externalId == null || externalId.isBlank()) {
        return null;
    }

    /*
     * Los IDs de work de Open Library normalmente
     * terminan en W.
     *
     * Si viene un ID de edición (por ejemplo termina en M),
     * no intentamos consultar /works/{id}/editions.json
     * porque Open Library respondería 404.
     */
    if (!externalId.endsWith("W")) {
        return null;
    }

    try {

        OpenLibraryEditionsResponse response = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/works/{workId}/editions.json")
                        .queryParam("limit", 10)
                        .build(externalId)
                )
                .retrieve()
                .body(OpenLibraryEditionsResponse.class);

        if (response == null
                || response.getEntries() == null) {
            return null;
        }

        for (OpenLibraryEditionDto edition :
                response.getEntries()) {

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

    } catch (Exception e) {

        /*
         * Una portada faltante no debe
         * romper toda la búsqueda.
         */
        return null;
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

        OpenLibraryWorkDto work =
                getWorkWithRetry(externalId);

        if (work == null) {
            throw new IllegalArgumentException(
                    "No se pudo obtener la información del libro"
            );
        }

        /*
        * Obtener los autores ya no podrá tumbar
        * todo el detalle si Open Library falla
        * únicamente en uno de ellos.
        */
        List<String> authors =
                getAuthorNames(work);

        String description =
                cleanDescription(
                        extractDescription(work)
                );

        String coverUrl = null;

        /*
        * Primero intentamos utilizar una portada
        * que ya venga directamente en el work.
        */
        if (work.getCovers() != null
                && !work.getCovers().isEmpty()) {

            coverUrl = buildCoverUrl(
                    work.getCovers().get(0),
                    "L"
            );
        }

        /*
        * Si no existe, buscamos una portada en
        * las ediciones.
        *
        * findCoverFromEditions ya es tolerante
        * a errores y devuelve null si falla.
        */
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

    private OpenLibraryWorkDto getWorkWithRetry(
            String externalId
    ) {

        /*
        * Hacemos máximo 2 intentos:
        *
        * intento 1 -> llamada normal
        * intento 2 -> pequeño reintento automático
        *
        * Así el usuario no tiene que regresar
        * y volver a tocar el libro manualmente.
        */
        for (int attempt = 1; attempt <= 2; attempt++) {

            try {

                return restClient
                        .get()
                        .uri(
                                "/works/{workId}.json",
                                externalId
                        )
                        .retrieve()
                        .body(
                                OpenLibraryWorkDto.class
                        );

            } catch (Exception e) {

                System.err.println(
                        "Error al consultar libro "
                                + externalId
                                + " en Open Library. Intento "
                                + attempt
                                + ": "
                                + e.getMessage()
                );

                /*
                * Si todavía nos queda un intento,
                * esperamos un momento antes
                * de repetir la solicitud.
                */
                if (attempt < 2) {

                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException interruptedException) {

                        Thread.currentThread()
                                .interrupt();

                        return null;
                    }
                }
            }
        }

        return null;
    }

    private List<String> getAuthorNames(
            OpenLibraryWorkDto work
    ) {

        List<String> authorNames =
                new ArrayList<>();

        JsonNode authorsNode =
                work.getAuthors();

        if (authorsNode == null
                || !authorsNode.isArray()) {

            return authorNames;
        }

        for (JsonNode authorEntry : authorsNode) {

            try {

                JsonNode authorNode =
                        authorEntry.get("author");

                if (authorNode == null) {
                    continue;
                }

                JsonNode keyNode =
                        authorNode.get("key");

                if (keyNode == null) {
                    continue;
                }

                String authorKey =
                        keyNode
                                .asText()
                                .replace(
                                        "/authors/",
                                        ""
                                );

                JsonNode authorResponse =
                        restClient
                                .get()
                                .uri(
                                        "/authors/{authorId}.json",
                                        authorKey
                                )
                                .retrieve()
                                .body(
                                        JsonNode.class
                                );

                if (authorResponse != null
                        && authorResponse.get("name") != null) {

                    authorNames.add(
                            authorResponse
                                    .get("name")
                                    .asText()
                    );
                }

            } catch (Exception e) {

                /*
                * Si Open Library falla al obtener
                * un autor, no debemos impedir que
                * el usuario vea el libro.
                */
                System.err.println(
                        "No se pudo obtener un autor: "
                                + e.getMessage()
                );
            }
        }

        return authorNames;
    }


    private String cleanDescription(String description) {

        if (description == null || description.isBlank()) {
            return null;
        }

        String cleaned = description;

        // **texto** -> texto
        cleaned = cleaned.replaceAll("\\*\\*(.*?)\\*\\*", "$1");

        // [texto](url) -> texto
        cleaned = cleaned.replaceAll(
                "\\[([^\\]]+)\\]\\([^\\)]+\\)",
                "$1"
        );

        // Elimina líneas tipo (Source: ...)
        cleaned = cleaned.replaceAll(
                "(?i)\\(Source:.*?\\)",
                ""
        );

        // Evita demasiados saltos seguidos
        cleaned = cleaned.replaceAll(
                "\\n{3,}",
                "\n\n"
        );

        return cleaned.trim();
    }

}