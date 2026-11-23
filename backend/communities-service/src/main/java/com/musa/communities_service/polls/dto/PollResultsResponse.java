package com.musa.communities_service.polls.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PollResultsResponse(
        UUID pollId,
        boolean open,
        long totalVotes,
        UUID selectedOptionId,
        OffsetDateTime generatedAt,
        List<OptionResult> options
) {

    public record OptionResult(
            UUID optionId,
            String label,
            short position,
            long voteCount
    ) {
    }
}