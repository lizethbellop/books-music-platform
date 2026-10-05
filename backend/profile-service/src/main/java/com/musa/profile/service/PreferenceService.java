package com.musa.profile.service;

import com.musa.profile.dto.AddPreferenceElementRequest;
import com.musa.profile.dto.PreferenceElementResponse;
import com.musa.profile.dto.PreferencesResponse;
import com.musa.profile.entity.Preference;
import com.musa.profile.entity.PreferenceElement;
import com.musa.profile.entity.Profile;
import com.musa.profile.exception.PreferenceElementAlreadyExistsException;
import com.musa.profile.repository.PreferenceElementRepository;
import com.musa.profile.repository.PreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.musa.profile.exception.PreferenceElementNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PreferenceService {

    private final ProfileService profileService;
    private final PreferenceRepository preferenceRepository;
    private final PreferenceElementRepository preferenceElementRepository;

    public PreferenceService(
            ProfileService profileService,
            PreferenceRepository preferenceRepository,
            PreferenceElementRepository preferenceElementRepository
    ) {
        this.profileService = profileService;
        this.preferenceRepository = preferenceRepository;
        this.preferenceElementRepository = preferenceElementRepository;
    }

    public PreferencesResponse getOwnPreferences(UUID userId) {
        Profile profile = profileService.getByUserId(userId);

        return preferenceRepository.findByProfileId(profile.getId())
                .map(preference -> PreferencesResponse.from(
                        preferenceElementRepository.findByPreferenceId(preference.getId())
                ))
                .orElseGet(() -> new PreferencesResponse(List.of()));
    }

    @Transactional
    public PreferenceElementResponse addElement(
            UUID userId,
            AddPreferenceElementRequest request
    ) {
        Profile profile = profileService.getByUserId(userId);

        Preference preference = preferenceRepository.findByProfileId(profile.getId())
                .orElseGet(() ->
                        preferenceRepository.save(new Preference(profile.getId()))
                );

        boolean alreadyExists =
                preferenceElementRepository.existsByPreferenceIdAndElementTypeAndReferenceId(
                        preference.getId(),
                        request.elementType(),
                        request.referenceId()
                );

        if (alreadyExists) {
            throw new PreferenceElementAlreadyExistsException();
        }

        PreferenceElement element = preferenceElementRepository.save(
                new PreferenceElement(
                        preference.getId(),
                        request.elementType(),
                        request.referenceId()
                )
        );

        return PreferenceElementResponse.from(element);
    }

    @Transactional
    public void removeElement(UUID userId, UUID elementId) {
        Profile profile = profileService.getByUserId(userId);

        Preference preference = preferenceRepository.findByProfileId(profile.getId())
                .orElseThrow(() -> new PreferenceElementNotFoundException(elementId));

        PreferenceElement element = preferenceElementRepository.findById(elementId)
                .orElseThrow(() -> new PreferenceElementNotFoundException(elementId));

        if (!element.getPreferenceId().equals(preference.getId())) {
            throw new PreferenceElementNotFoundException(elementId);
        }

        preferenceElementRepository.delete(element);
    }
}