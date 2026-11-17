package com.musa.social.mapper;

import com.musa.social.dto.response.PostResponse;
import com.musa.social.entity.Post;

public final class PostMapper {

    private PostMapper() {
    }

    public static PostResponse toResponse(Post post) {

        return new PostResponse(
                post.getId(),
                post.getAuthorId(),
                post.getText(),
                post.getPostOrigin(),
                post.getActivityType(),
                post.getContentType(),
                post.getReferenceId(),
                post.getActivityData(),
                post.getCommunityId(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getDeletedAt()
        );
    }
}