package com.musa.social.service;

import com.musa.social.dto.request.CreateCommentRequest;
import com.musa.social.dto.request.UpdateCommentRequest;
import com.musa.social.dto.response.CommentResponse;

import java.util.List;
import java.util.UUID;

public interface CommentService {

    CommentResponse createComment(
            UUID authenticatedUserId,
            UUID postId,
            CreateCommentRequest request
    );

    CommentResponse updateComment(
            UUID authenticatedUserId,
            UUID commentId,
            UpdateCommentRequest request
    );

    void deleteComment(
            UUID authenticatedUserId,
            UUID commentId
    );

    List<CommentResponse> getCommentsByPost(
            UUID postId
    );
}