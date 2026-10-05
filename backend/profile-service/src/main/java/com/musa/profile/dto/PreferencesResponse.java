package com.musa.profile.dto;

import com.musa.profile.entity.PreferenceElement;

import java.util.List;

public record PreferencesResponse(
        List<PreferenceElementResponse> elements
) {
    public static PreferencesResponse from(List<PreferenceElement> elements) {
        return new PreferencesResponse(
                elements.stream()
                        .map(PreferenceElementResponse::from)
                        .toList()
        );
    }
}
