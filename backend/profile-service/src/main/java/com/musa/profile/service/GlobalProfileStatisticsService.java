package com.musa.profile.service;

import com.musa.profile.dto.GlobalProfileStatisticsResponse;
import com.musa.profile.repository.ListElementRepository;
import com.musa.profile.repository.PreferenceElementRepository;
import com.musa.profile.repository.UserListRepository;
import org.springframework.stereotype.Service;

@Service
public class GlobalProfileStatisticsService {

    private final UserListRepository userListRepository;
    private final ListElementRepository listElementRepository;
    private final PreferenceElementRepository preferenceElementRepository;

    public GlobalProfileStatisticsService(
            UserListRepository userListRepository,
            ListElementRepository listElementRepository,
            PreferenceElementRepository preferenceElementRepository
    ) {
        this.userListRepository = userListRepository;
        this.listElementRepository = listElementRepository;
        this.preferenceElementRepository = preferenceElementRepository;
    }

    public GlobalProfileStatisticsResponse getGlobalStatistics() {

        long lists = userListRepository.count();

        long listElements = listElementRepository.count();

        long preferenceElements =
                preferenceElementRepository.count();

        return new GlobalProfileStatisticsResponse(
                lists,
                listElements,
                preferenceElements
        );
    }
}