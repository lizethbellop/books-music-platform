package com.musa.profile.service;

import java.util.UUID;

import com.musa.profile.dto.ProfileStatisticsResponse;
import com.musa.profile.entity.Profile;
import com.musa.profile.repository.ListElementRepository;
import com.musa.profile.repository.PreferenceElementRepository;
import com.musa.profile.repository.ProfileRepository;
import com.musa.profile.repository.UserListRepository;
import org.springframework.stereotype.Service;

@Service
public class ProfileStatisticsService {

    private final ProfileRepository profileRepository;
    private final UserListRepository userListRepository;
    private final ListElementRepository listElementRepository;
    private final PreferenceElementRepository preferenceElementRepository;

    public ProfileStatisticsService(
            ProfileRepository profileRepository,
            UserListRepository userListRepository,
            ListElementRepository listElementRepository,
            PreferenceElementRepository preferenceElementRepository
    ) {
        this.profileRepository = profileRepository;
        this.userListRepository = userListRepository;
        this.listElementRepository = listElementRepository;
        this.preferenceElementRepository = preferenceElementRepository;
    }

    public ProfileStatisticsResponse getUserStatistics(UUID userId) {

        Profile profile = profileRepository
                .findByUserId(userId)
                .orElse(null);

        if (profile == null) {
            return new ProfileStatisticsResponse(0, 0, 0);
        }

        UUID profileId = profile.getId();

        long lists =
                userListRepository.countByProfileId(profileId);

        long listElements =
                listElementRepository.countByProfileId(profileId);

        long preferenceElements =
                preferenceElementRepository.countByProfileId(profileId);

        return new ProfileStatisticsResponse(
                lists,
                listElements,
                preferenceElements
        );
    }
}