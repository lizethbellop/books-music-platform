package com.musa.profile.service;

import com.musa.profile.entity.Profile;
import com.musa.profile.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnsureOwnProfileTest {

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void debeDevolverPerfilCreadoCuandoNoExistia() {
        UUID userId = UUID.randomUUID();
        Profile createdProfile = new Profile(userId, false);

        when(profileRepository.insertIfAbsent(
                any(UUID.class), eq(userId)
        )).thenReturn(1);

        when(profileRepository.findByUserId(userId))
                .thenReturn(Optional.of(createdProfile));

        Profile result = profileService.ensureOwnProfile(userId);

        assertSame(createdProfile, result);

        var order = inOrder(profileRepository);
        order.verify(profileRepository)
                .insertIfAbsent(any(UUID.class), eq(userId));
        order.verify(profileRepository).findByUserId(userId);
    }

    @Test
    void debeDevolverPerfilExistenteSinCambiarSusDatos() {
        UUID userId = UUID.randomUUID();
        Profile existingProfile = new Profile(userId, true);
        existingProfile.setBiography("Mi biografía");
        existingProfile.setProfilePictureUrl(
                "https://example.com/photo.jpg"
        );

        when(profileRepository.insertIfAbsent(
                any(UUID.class), eq(userId)
        )).thenReturn(0);

        when(profileRepository.findByUserId(userId))
                .thenReturn(Optional.of(existingProfile));

        Profile result = profileService.ensureOwnProfile(userId);

        assertSame(existingProfile, result);
        verify(profileRepository)
                .insertIfAbsent(any(UUID.class), eq(userId));
        verify(profileRepository).findByUserId(userId);
        verifyNoMoreInteractions(profileRepository);
    }
}