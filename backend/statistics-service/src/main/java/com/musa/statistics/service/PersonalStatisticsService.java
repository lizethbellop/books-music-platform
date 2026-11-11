package com.musa.statistics.service;

import java.util.UUID;

import com.musa.statistics.client.BookStatisticsClient;
import com.musa.statistics.client.MusicStatisticsClient;
import com.musa.statistics.client.ProfileStatisticsClient;
import com.musa.statistics.dto.BookStatisticsResponse;
import com.musa.statistics.dto.MusicStatisticsResponse;
import com.musa.statistics.dto.PersonalStatisticsResponse;
import com.musa.statistics.dto.ProfileStatisticsResponse;
import org.springframework.stereotype.Service;

@Service
public class PersonalStatisticsService {

    private final MusicStatisticsClient musicStatisticsClient;
    private final BookStatisticsClient bookStatisticsClient;
    private final ProfileStatisticsClient profileStatisticsClient;

    public PersonalStatisticsService(
            MusicStatisticsClient musicStatisticsClient,
            BookStatisticsClient bookStatisticsClient,
            ProfileStatisticsClient profileStatisticsClient
    ) {
        this.musicStatisticsClient = musicStatisticsClient;
        this.bookStatisticsClient = bookStatisticsClient;
        this.profileStatisticsClient = profileStatisticsClient;
    }

    public PersonalStatisticsResponse getPersonalStatistics(
            UUID userId,
            String authorizationHeader
    ) {
        MusicStatisticsResponse music =
                musicStatisticsClient
                        .getMyStatistics(authorizationHeader);

        BookStatisticsResponse books =
                bookStatisticsClient
                        .getMyStatistics(authorizationHeader);

        ProfileStatisticsResponse profile =
                profileStatisticsClient
                        .getMyStatistics(authorizationHeader);

        return new PersonalStatisticsResponse(
                userId,
                music,
                books,
                profile
        );
    }
}