package com.musa.social.controller;

import com.musa.social.config.AuthenticatedUser;
import com.musa.social.dto.request.CreateCommentRequest;
import com.musa.social.dto.request.UpdateCommentRequest;
import com.musa.social.dto.response.CommentResponse;
import com.musa.social.service.CommentService;

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
@RequestMapping("/api/v1/social")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;


    // Agregar comentario
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID authenticatedUserId =
                AuthenticatedUser.getUserId(jwt);

        CommentResponse response =
                commentService.createComment(
                        authenticatedUserId,
                        postId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // Consultar comentarios
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getCommentsByPost(
            @PathVariable UUID postId
    ) {

        return ResponseEntity.ok(
                commentService.getCommentsByPost(postId)
        );
    }


    // Editar comentario
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID authenticatedUserId =
                AuthenticatedUser.getUserId(jwt);

        return ResponseEntity.ok(
                commentService.updateComment(
                        authenticatedUserId,
                        commentId,
                        request
                )
        );
    }


    // Eliminar comentario
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID authenticatedUserId =
                AuthenticatedUser.getUserId(jwt);

        commentService.deleteComment(
                authenticatedUserId,
                commentId
        );

        return ResponseEntity.noContent().build();
    }
}