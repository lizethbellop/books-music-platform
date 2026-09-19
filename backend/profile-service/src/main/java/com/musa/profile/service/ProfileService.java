package com.musa.profile.service;

import com.musa.profile.entity.Profile;
import com.musa.profile.exception.ProfileNotFoundException;
import com.musa.profile.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository){
        this.profileRepository = profileRepository;
    }

    public Profile getByUserId(UUID userId){
        return profileRepository.findByUserId(userId).orElseThrow(() -> new ProfileNotFoundException(userId));
    }
}
