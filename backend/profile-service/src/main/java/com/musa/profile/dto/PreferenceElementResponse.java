package com.musa.profile.dto;

import com.musa.profile.entity.PreferenceElement;
import java.util.UUID;

public record PreferenceElementResponse(
        UUID id,
        String elementType,
        String referenceId
) {
    public static PreferenceElementResponse from(PreferenceElement element) {
        return new PreferenceElementResponse(
                element.getId(),
                element.getElementType(),
                element.getReferenceId()
        );
    }
}
