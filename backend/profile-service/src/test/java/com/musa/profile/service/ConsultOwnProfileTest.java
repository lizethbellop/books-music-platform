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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CU-04 Consultar perfil")
@ExtendWith(MockitoExtension.class)
class ConsultOwnProfileTest {

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    @DisplayName("Returns the profile when it exists")
    void shouldReturnProfileWhenUserExists(){
        UUID userId = UUID.randomUUID();
        Profile expectedProfile = new Profile(userId, false);

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(expectedProfile));

        Profile result = profileService.getByUserId(userId);

        assertSame(expectedProfile, result);
        verify(profileRepository).findByUserId(userId);
    }

    void shouldThrowExceptionWhenPRofileDoesNotExist(){
        UUID userId = UUID.randomUUID();

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class, () -> profileService.getByUserId(userId));
        verify(profileRepository).findByUserId(userId);
    }
}
