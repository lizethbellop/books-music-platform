package com.musa.social.service;

import com.musa.social.dto.request.CreatePostRequest;
import com.musa.social.dto.request.UpdatePostRequest;
import com.musa.social.dto.response.PostResponse;

import java.util.List;
import java.util.UUID;

public interface PostService {

    PostResponse createPost(
            UUID authenticatedUserId,
            CreatePostRequest request
    );

    PostResponse updatePost(
            UUID authenticatedUserId,
            UUID postId,
            UpdatePostRequest request
    );

    // Soft delete
    void deletePost(
            UUID authenticatedUserId,
            UUID postId
    );

    // Restaurar publicación eliminada
    PostResponse restorePost(
            UUID authenticatedUserId,
            UUID postId
    );

    // Eliminación física inmediata
    void permanentlyDeletePost(
            UUID authenticatedUserId,
            UUID postId
    );

    // Eliminados recientemente
    List<PostResponse> getDeletedPosts(
            UUID authenticatedUserId
    );

    PostResponse getPostById(
            UUID postId
    );

    List<PostResponse> getPostsByUser(
            UUID userId
    );

    List<PostResponse> getPostsByCommunity(
            UUID communityId
    );
    
    List<PostResponse> getUserActivity(
            UUID userId
        );
}