package com.musa.social.controller;

import com.musa.social.config.AuthenticatedUser;
import com.musa.social.dto.response.PostLikeResponse;
import com.musa.social.service.PostLikeService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social/posts")
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService postLikeService;


    // Dar like
    @PostMapping("/{postId}/likes")
    public ResponseEntity<PostLikeResponse> likePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID authenticatedUserId =
                AuthenticatedUser.getUserId(jwt);

        PostLikeResponse response =
                postLikeService.likePost(
                        authenticatedUserId,
                        postId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // Quitar like
    @DeleteMapping("/{postId}/likes")
    public ResponseEntity<Void> unlikePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID authenticatedUserId =
                AuthenticatedUser.getUserId(jwt);

        postLikeService.unlikePost(
                authenticatedUserId,
                postId
        );

        return ResponseEntity.noContent().build();
    }


    // Consultar cantidad de likes
    @GetMapping("/{postId}/likes/count")
    public ResponseEntity<Long> countLikes(
            @PathVariable UUID postId
    ) {

        return ResponseEntity.ok(
                postLikeService.countLikes(postId)
        );
    }


    // Consultar likes
    @GetMapping("/{postId}/likes")
    public ResponseEntity<List<PostLikeResponse>> getPostLikes(
            @PathVariable UUID postId
    ) {

        return ResponseEntity.ok(
                postLikeService.getPostLikes(postId)
        );
    }
}