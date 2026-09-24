package com.musa.profile.dto;

import com.musa.profile.entity.ListElement;

import java.util.UUID;

public record ListElementResponse(
        UUID id,
        String elementType,
        String referenceId
) {
    public static ListElementResponse from(ListElement element) {
        return new ListElementResponse(
                element.getId(),
                element.getElementType(),
                element.getReferenceId()
        );
    }
}