package com.musa.profile.dto;

import com.musa.profile.entity.PreferenceElement;
import java.util.UUID;
import com.musa.profile.dto.catalog.CatalogResolution;

public record PreferenceElementResponse(
        UUID id,
        String elementType,
        String referenceId,
        CatalogResolution.Status resolutionStatus,
        Object content
) {
    public PreferenceElementResponse(UUID id, String elementType, String referenceId) {
        this(id, elementType, referenceId, CatalogResolution.Status.UNAVAILABLE, null);
    }

    public static PreferenceElementResponse from(PreferenceElement element) {
        return new PreferenceElementResponse(
                element.getId(),
                element.getElementType(),
                element.getReferenceId()
        );
    }
}
