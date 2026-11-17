package com.musa.social.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PostLikeResponse(

        UUID id,
        UUID postId,
        UUID userId,
        OffsetDateTime createdAt

) {
}