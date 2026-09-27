package com.musa.profile.controller;

import com.musa.profile.dto.ProfileResponse;
import com.musa.profile.service.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.musa.profile.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import com.musa.profile.service.ProfilePhotoService;

import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {
    private final ProfileService profileService;
    private final ProfilePhotoService profilePhotoService;

    public ProfileController(ProfileService profileService, ProfilePhotoService profilePhotoService){
        this.profileService = profileService;
        this.profilePhotoService = profilePhotoService;
    }

    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse uploadOwnPhoto(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestPart("file") MultipartFile file
    ) {
        return ProfileResponse.from(profilePhotoService.upload(userId, file));
    }

    @GetMapping("/me")
    public ProfileResponse getOwnProfile(
            @RequestHeader("X-User-Id") UUID userId
    ){
        return ProfileResponse.from(
              profileService.getByUserId(userId)
        );
    }

    @PutMapping("/me")
    public ProfileResponse updateOwnProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ProfileResponse.from(
                profileService.updateProfile(
                        userId,
                        request.biography(),
                        request.privateProfile()
                )
        );
    }

}
