package com.musa.books.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient openLibraryRestClient(
            @Value("${openlibrary.api-url}") String apiUrl
    ) {
        return RestClient.builder()
                .baseUrl(apiUrl)
                .build();
    }
}