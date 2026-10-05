package com.musa.profile.controller;

import com.musa.profile.dto.AddPreferenceElementRequest;
import com.musa.profile.dto.PreferenceElementResponse;
import com.musa.profile.dto.PreferencesResponse;
import com.musa.profile.service.PreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/profiles/me/preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public PreferencesResponse getOwnPreferences(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return preferenceService.getOwnPreferences(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PreferenceElementResponse addElement(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody AddPreferenceElementRequest request
    ) {
        return preferenceService.addElement(userId, request);
    }

    @DeleteMapping("/{elementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeElement(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID elementId
    ) {
        preferenceService.removeElement(userId, elementId);
    }
}