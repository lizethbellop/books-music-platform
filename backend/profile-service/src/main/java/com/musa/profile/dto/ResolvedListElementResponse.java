package com.musa.profile.dto;

import java.util.UUID;

import com.musa.profile.dto.catalog.CatalogResolution;
import com.musa.profile.entity.ListElement;

public record ResolvedListElementResponse(
        UUID id,
        String elementType,
        String referenceId,
        CatalogResolution.Status resolutionStatus,
        Object content
) {

    public static ResolvedListElementResponse from(
            ListElement element,
            CatalogResolution resolution
    ) {
        return new ResolvedListElementResponse(
                element.getId(),
                element.getElementType(),
                element.getReferenceId(),
                resolution.resolutionStatus(),
                resolution.content()
        );
    }
}