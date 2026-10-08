package com.musa.books.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class TranslationService {

    private final RestClient restClient;
    private final String apiKey;

    public TranslationService(
            @Value("${deepl.api-key}") String apiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api-free.deepl.com")
                .build();

        this.apiKey = apiKey;
    }

    public String translateToSpanish(String text) {

        if (text == null || text.isBlank()) {
            return text;
        }

        DeepLResponse response = restClient
                .post()
                .uri("/v2/translate")
                .header(
                        "Authorization",
                        "DeepL-Auth-Key " + apiKey
                )
                .body(
                        Map.of(
                                "text", List.of(text),
                                "target_lang", "ES"
                        )
                )
                .retrieve()
                .body(DeepLResponse.class);

        if (response == null
                || response.translations() == null
                || response.translations().isEmpty()) {

            return text;
        }

        return response.translations()
                .get(0)
                .text();
    }

    public String translateToEnglish(String text) {

        if (text == null || text.isBlank()) {
            return text;
        }

        DeepLResponse response = restClient
                .post()
                .uri("/v2/translate")
                .header(
                        "Authorization",
                        "DeepL-Auth-Key " + apiKey
                )
                .body(
                        Map.of(
                                "text", List.of(text),
                                "target_lang", "EN"
                        )
                )
                .retrieve()
                .body(DeepLResponse.class);

        if (response == null
                || response.translations() == null
                || response.translations().isEmpty()) {

            return text;
        }

        return response.translations()
                .get(0)
                .text();
    }

    private record DeepLResponse(
            List<DeepLTranslation> translations
    ) {
    }

    private record DeepLTranslation(
            String detected_source_language,
            String text
    ) {
    }
}