package com.musa.statistics.client;

import com.musa.statistics.dto.BookStatisticsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BookStatisticsClient {

    private final RestClient restClient;

    public BookStatisticsClient(
            RestClient.Builder builder,
            @Value("${musa.services.books-url}") String booksServiceUrl
    ) {
        this.restClient = builder
                .baseUrl(booksServiceUrl)
                .build();
    }

    public BookStatisticsResponse getMyStatistics(
            String authorizationHeader
    ) {
        return restClient
                .get()
                .uri("/books/statistics/me")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        authorizationHeader
                )
                .retrieve()
                .body(BookStatisticsResponse.class);
    }
}