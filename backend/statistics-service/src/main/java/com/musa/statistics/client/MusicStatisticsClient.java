package com.musa.statistics.client;

import com.musa.statistics.dto.MusicStatisticsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MusicStatisticsClient {

    private final RestClient restClient;

    public MusicStatisticsClient(
            RestClient.Builder builder,
            @Value("${musa.services.music-url}") String musicServiceUrl
    ) {
        this.restClient = builder
                .baseUrl(musicServiceUrl)
                .build();
    }

    public MusicStatisticsResponse getMyStatistics(
            String authorizationHeader
    ) {
        return restClient
                .get()
                .uri("/api/music/statistics/me")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        authorizationHeader
                )
                .retrieve()
                .body(MusicStatisticsResponse.class);
    }
}