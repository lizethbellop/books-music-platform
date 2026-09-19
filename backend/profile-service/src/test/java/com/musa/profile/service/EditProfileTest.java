package com.musa.profile.service;

import com.musa.profile.entity.Profile;
import com.musa.profile.exception.ProfileNotFoundException;
import com.musa.profile.repository.ProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Editar perfil")
@ExtendWith(MockitoExtension.class)
class EditProfileTest {
    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    @DisplayName("Updates biography and privacy")
    void shouldUpdateBiographyAndPrivacy(){
        UUID userId = UUID.randomUUID();

        Profile existingProfile = new Profile(userId, false);
        existingProfile.setBiography("Old biography");

        Profile expectedProfile = new Profile(userId, true);
        expectedProfile.setBiography("New biography");

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));

        Profile result = profileService.updateProfile(userId, "New biography", true);

        assertThat(result).usingRecursiveComparison().isEqualTo(expectedProfile);
    }

    @Test
    @DisplayName("Throws an exception when the profile does not exist")
    void shouldThrowExceptionWhenProfileDoesNotExist(){
        UUID userId = UUID.randomUUID();

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class, () -> profileService.updateProfile(userId, "New biography", true));
    }
}
