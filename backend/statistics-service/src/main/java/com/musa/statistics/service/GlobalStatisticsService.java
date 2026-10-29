package com.musa.statistics.service;

import com.musa.statistics.client.BookStatisticsClient;
import com.musa.statistics.client.MusicStatisticsClient;
import com.musa.statistics.client.ProfileStatisticsClient;
import com.musa.statistics.dto.GlobalBookStatisticsResponse;
import com.musa.statistics.dto.GlobalMusicStatisticsResponse;
import com.musa.statistics.dto.GlobalProfileStatisticsResponse;
import com.musa.statistics.dto.GlobalStatisticsResponse;
import org.springframework.stereotype.Service;

@Service
public class GlobalStatisticsService {

    private final MusicStatisticsClient musicStatisticsClient;
    private final BookStatisticsClient bookStatisticsClient;
    private final ProfileStatisticsClient profileStatisticsClient;

    public GlobalStatisticsService(
            MusicStatisticsClient musicStatisticsClient,
            BookStatisticsClient bookStatisticsClient,
            ProfileStatisticsClient profileStatisticsClient
    ) {
        this.musicStatisticsClient = musicStatisticsClient;
        this.bookStatisticsClient = bookStatisticsClient;
        this.profileStatisticsClient = profileStatisticsClient;
    }

    public GlobalStatisticsResponse getGlobalStatistics(
            String authorizationHeader
    ) {

        GlobalMusicStatisticsResponse music =
                musicStatisticsClient.getGlobalStatistics(
                        authorizationHeader
                );

        GlobalBookStatisticsResponse books =
                bookStatisticsClient.getGlobalStatistics(
                        authorizationHeader
                );

        GlobalProfileStatisticsResponse profile =
                profileStatisticsClient.getGlobalStatistics(
                        authorizationHeader
                );

        return new GlobalStatisticsResponse(
                music,
                books,
                profile
        );
    }
}