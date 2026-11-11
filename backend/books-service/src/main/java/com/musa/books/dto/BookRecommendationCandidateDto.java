package com.musa.books.dto;

import java.util.List;

public record BookRecommendationCandidateDto(
        String externalId,
        String title,
        List<String> authors,
        Integer firstPublishYear,
        String coverUrl,
        List<String> subjects
) {}
