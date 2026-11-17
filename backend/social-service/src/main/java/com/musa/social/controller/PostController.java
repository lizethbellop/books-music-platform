package com.musa.social.controller;

import com.musa.social.config.AuthenticatedUser;
import com.musa.social.dto.request.CreatePostRequest;
import com.musa.social.dto.request.UpdatePostRequest;
import com.musa.social.dto.response.PostResponse;
import com.musa.social.service.PostService;

import jakarta.validation.Valid;

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
public class PostController {

    private final PostService postService;


    // Crear publicación
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID authenticatedUserId =
                AuthenticatedUser.getUserId(jwt);

        PostResponse response =
                postService.createPost(
                        authenticatedUserId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // Consultar publicación por ID
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPostById(
            @PathVariable UUID postId
    ) {

        return ResponseEntity.ok(
                postService.getPostById(postId)
        );
    }

    // Publicaciones de usuario
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostResponse>> getPostsByUser(
            @PathVariable UUID userId
    ) {

        return ResponseEntity.ok(
                postService.getPostsByUser(userId)
        );
    }

    // Publicaciones de comunidad
    @GetMapping("/community/{communityId}")
    public ResponseEntity<List<PostResponse>> getPostsByCommunity(
            @PathVariable UUID communityId
    ) {

        return ResponseEntity.ok(
                postService.getPostsByCommunity(communityId)
        );
    }

    // Editar publicación
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID authenticatedUserId =
                AuthenticatedUser.getUserId(jwt);

        PostResponse response =
                postService.updatePost(
                        authenticatedUserId,
                        postId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    // Eliminar publicación temporalmente
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID authenticatedUserId =
                AuthenticatedUser.getUserId(jwt);

        postService.deletePost(
                authenticatedUserId,
                postId
        );

        return ResponseEntity.noContent().build();
    }

    // Consultar eliminados recientemente
    @GetMapping("/deleted")
    public ResponseEntity<List<PostResponse>> getDeletedPosts(
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID authenticatedUserId =
                AuthenticatedUser.getUserId(jwt);

        return ResponseEntity.ok(
                postService.getDeletedPosts(
                        authenticatedUserId
                )
        );
    }

    // Restaurar publicación
    @PostMapping("/{postId}/restore")
    public ResponseEntity<PostResponse> restorePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID authenticatedUserId =
                AuthenticatedUser.getUserId(jwt);

        return ResponseEntity.ok(
                postService.restorePost(
                        authenticatedUserId,
                        postId
                )
        );
    }

    // Eliminar publicación permanentemente
    @DeleteMapping("/{postId}/permanent")
    public ResponseEntity<Void> permanentlyDeletePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID authenticatedUserId =
                AuthenticatedUser.getUserId(jwt);

        postService.permanentlyDeletePost(
                authenticatedUserId,
                postId
        );

        return ResponseEntity.noContent().build();
    }
}