package com.musa.profile.dto.catalog;

import java.util.List;

public record BookCatalogResponse(
        String externalId,
        String title,
        List<String> authors,
        String description,
        String firstPublishDate,
        List<String> subjects,
        String coverUrl
) {
}