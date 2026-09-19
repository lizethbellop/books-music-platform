package com.musa.profile.controller;

import com.musa.profile.dto.ProfileResponse;
import com.musa.profile.service.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService){
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ProfileResponse getOwnProfile(
            @RequestHeader("X-User-Id") UUID userId
    ){
        return ProfileResponse.from(
              profileService.getByUserId(userId)
        );
    }

}
