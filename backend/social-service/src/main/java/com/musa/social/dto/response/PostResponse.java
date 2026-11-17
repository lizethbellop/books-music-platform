package com.musa.social.dto.response;

import com.musa.social.entity.ActivityType;
import com.musa.social.entity.ContentType;
import com.musa.social.entity.PostOrigin;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record PostResponse(

        UUID id,
        UUID authorId,
        String text,

        PostOrigin postOrigin,
        ActivityType activityType,

        ContentType contentType,
        String referenceId,

        Map<String, Object> activityData,

        UUID communityId,

        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {
}