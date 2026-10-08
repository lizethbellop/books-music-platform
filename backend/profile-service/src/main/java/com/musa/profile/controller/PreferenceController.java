package com.musa.profile.controller;

import com.musa.profile.dto.AddPreferenceElementRequest;
import com.musa.profile.dto.PreferenceElementResponse;
import com.musa.profile.dto.PreferencesResponse;
import com.musa.profile.service.PreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/profiles/me/preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public PreferencesResponse getOwnPreferences(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return preferenceService.getOwnPreferences(userId, jwt.getTokenValue());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PreferenceElementResponse addElement(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddPreferenceElementRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return preferenceService.addElement(userId, request);
    }

    @DeleteMapping("/{elementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeElement(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID elementId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        preferenceService.removeElement(userId, elementId);
    }
}