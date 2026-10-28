package com.musa.statistics.client;

import com.musa.statistics.dto.GlobalProfileStatisticsResponse;
import com.musa.statistics.dto.ProfileStatisticsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.musa.statistics.dto.GlobalProfileStatisticsResponse;

@Component
public class ProfileStatisticsClient {

    private final RestClient restClient;

    public ProfileStatisticsClient(
            RestClient.Builder builder,
            @Value("${musa.services.profile-url}") String profileServiceUrl
    ) {
        this.restClient = builder
                .baseUrl(profileServiceUrl)
                .build();
    }

    public ProfileStatisticsResponse getMyStatistics(
            String authorizationHeader
    ) {
        return restClient
                .get()
                .uri("/api/profiles/statistics/me")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        authorizationHeader
                )
                .retrieve()
                .body(ProfileStatisticsResponse.class);
    }

    public GlobalProfileStatisticsResponse getGlobalStatistics(
            String authorizationHeader
    ) {
        return restClient
                .get()
                .uri("/api/profiles/statistics/global")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        authorizationHeader
                )
                .retrieve()
                .body(GlobalProfileStatisticsResponse.class);
    }
}