package com.musa.social.mapper;

import com.musa.social.dto.response.PostLikeResponse;
import com.musa.social.entity.PostLike;

public final class PostLikeMapper {

    private PostLikeMapper() {
    }

    public static PostLikeResponse toResponse(
            PostLike postLike
    ) {

        return new PostLikeResponse(
                postLike.getId(),
                postLike.getPostId(),
                postLike.getUserId(),
                postLike.getCreatedAt()
        );
    }
}