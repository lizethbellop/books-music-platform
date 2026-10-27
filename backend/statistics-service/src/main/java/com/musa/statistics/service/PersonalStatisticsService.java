package com.musa.statistics.service;

import java.util.UUID;

import com.musa.statistics.client.MusicStatisticsClient;
import com.musa.statistics.dto.MusicStatisticsResponse;
import com.musa.statistics.dto.PersonalStatisticsResponse;
import org.springframework.stereotype.Service;

@Service
public class PersonalStatisticsService {

    private final MusicStatisticsClient musicStatisticsClient;

    public PersonalStatisticsService(
            MusicStatisticsClient musicStatisticsClient
    ) {
        this.musicStatisticsClient =
                musicStatisticsClient;
    }

    public PersonalStatisticsResponse getPersonalStatistics(
            UUID userId,
            String authorizationHeader
    ) {
        MusicStatisticsResponse music =
                musicStatisticsClient
                        .getMyStatistics(
                                authorizationHeader
                        );

        return new PersonalStatisticsResponse(
                userId,
                music
        );
    }
}