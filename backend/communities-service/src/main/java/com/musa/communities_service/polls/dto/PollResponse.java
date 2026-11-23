package com.musa.communities_service.polls.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PollResponse(
        UUID id,
        UUID communityId,
        UUID createdBy,
        String question,
        OffsetDateTime createdAt,
        OffsetDateTime closesAt,
        OffsetDateTime closedAt,
        boolean open,
        List<Option> options
) {

    public record Option(
            UUID id,
            String label,
            short position
    ) {
    }
}