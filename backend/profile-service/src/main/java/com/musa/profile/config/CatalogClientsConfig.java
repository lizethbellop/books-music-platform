package com.musa.profile.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class CatalogClientsConfig {

    @Bean
    public RestClient booksCatalogRestClient(
            @Value("${musa.services.books.base-url}") String baseUrl
    ) {
        return createClient(baseUrl);
    }

    @Bean
    public RestClient musicCatalogRestClient(
            @Value("${musa.services.music.base-url}") String baseUrl
    ) {
        return createClient(baseUrl);
    }

    private RestClient createClient(String baseUrl) {
        var requestFactory = new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}