package com.musa.social.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommentResponse(

        UUID id,
        UUID postId,
        UUID authorId,
        String text,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt

) {
}