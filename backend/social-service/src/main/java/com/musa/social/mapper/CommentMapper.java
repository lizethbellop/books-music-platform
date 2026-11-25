package com.musa.social.mapper;

import com.musa.social.dto.response.CommentResponse;
import com.musa.social.entity.Comment;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static CommentResponse toResponse(
            Comment comment
    ) {

        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorId(),
                comment.getText(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}