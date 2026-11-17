package com.musa.social.dto.request;

import com.musa.social.entity.ActivityType;
import com.musa.social.entity.ContentType;
import com.musa.social.entity.PostOrigin;

import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record CreatePostRequest(

        @Size(max = 5000, message = "El texto de la publicación es demasiado largo")
        String text,

        PostOrigin postOrigin,

        ActivityType activityType,

        ContentType contentType,

        String referenceId,

        Map<String, Object> activityData,

        UUID communityId
) {
}