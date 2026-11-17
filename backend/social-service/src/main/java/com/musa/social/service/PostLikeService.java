package com.musa.social.service;

import com.musa.social.dto.response.PostLikeResponse;

import java.util.List;
import java.util.UUID;

public interface PostLikeService {

    PostLikeResponse likePost(
            UUID authenticatedUserId,
            UUID postId
    );

    void unlikePost(
            UUID authenticatedUserId,
            UUID postId
    );

    long countLikes(
            UUID postId
    );

    List<PostLikeResponse> getPostLikes(
            UUID postId
    );
}