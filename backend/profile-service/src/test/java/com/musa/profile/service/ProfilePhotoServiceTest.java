package com.musa.profile.service;

import com.musa.profile.entity.Profile;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfilePhotoServiceTest {
    private final UUID userId = UUID.randomUUID();
    private final ProfileService profileService = mock(ProfileService.class);

    @Test
    void rejectsNonImageUpload() {
        when(profileService.getByUserId(userId)).thenReturn(new Profile(userId, false));
        var service = new ProfilePhotoService(profileService, "not-used");
        var file = new MockMultipartFile("file", "test.txt", "text/plain", new byte[]{1});

        var error = assertThrows(ResponseStatusException.class,
                () -> service.upload(userId, file));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void reportsMissingCloudinaryConfiguration() {
        when(profileService.getByUserId(userId)).thenReturn(new Profile(userId, false));
        var service = new ProfilePhotoService(profileService, "");
        var file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1});

        var error = assertThrows(ResponseStatusException.class,
                () -> service.upload(userId, file));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }
}
