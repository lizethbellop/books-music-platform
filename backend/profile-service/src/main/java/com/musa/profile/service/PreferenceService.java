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

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private final ProfileService profileService;
    private final CatalogLookupService catalogLookupService;
    private final PreferenceRepository preferenceRepository;
    private final PreferenceElementRepository preferenceElementRepository;

    public PreferenceService(
            ProfileService profileService,
            PreferenceRepository preferenceRepository,
            PreferenceElementRepository preferenceElementRepository,
            CatalogLookupService catalogLookupService
    ) {
        this.profileService = profileService;
        this.catalogLookupService = catalogLookupService;
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

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public PreferencesResponse getOwnPreferences(UUID userId, String accessToken) {
        PreferencesResponse stored = getOwnPreferences(userId);
        return new PreferencesResponse(stored.elements().stream().map(element -> {
            var resolution = catalogLookupService.resolve(
                    element.elementType(), element.referenceId(), accessToken);
            return new PreferenceElementResponse(element.id(), element.elementType(),
                    element.referenceId(), resolution.resolutionStatus(), resolution.content());
        }).toList());
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

    /** Atomic and retry-safe: an already stored selection is a successful no-op. */
    @Transactional
    public PreferencesResponse saveInitialPreferences(
            UUID userId, com.musa.profile.dto.SaveInitialPreferencesRequest request
    ) {
        Profile profile = profileService.getByUserId(userId);
        // Serialize onboarding batches, including first creation of the preferences row.
        entityManager.lock(profile, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        Preference preference = preferenceRepository.findByProfileId(profile.getId())
                .orElseGet(() -> preferenceRepository.save(new Preference(profile.getId())));
        var existing = new java.util.ArrayList<>(
                preferenceElementRepository.findByPreferenceId(preference.getId()));
        var keys = new java.util.HashSet<String>();
        existing.forEach(e -> keys.add(e.getElementType()+":"+e.getReferenceId()));
        var added = new java.util.ArrayList<PreferenceElement>();
        for (var selection : request.elements()) {
            String reference = selection.referenceId().trim();
            if (selection.elementType().equals("BOOK") && !reference.matches("OL[0-9]+W")) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Se requiere una obra OpenLibrary válida.");
            }
            if (keys.add(selection.elementType()+":"+reference)) {
                added.add(new PreferenceElement(preference.getId(), selection.elementType(), reference));
            }
        }
        existing.addAll(preferenceElementRepository.saveAllAndFlush(added));
        return PreferencesResponse.from(existing);
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