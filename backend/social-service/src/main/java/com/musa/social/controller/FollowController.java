package com.musa.social.controller;

import com.musa.social.config.AuthenticatedUser;
import com.musa.social.entity.Follow;
import com.musa.social.service.FollowService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    // =========================================================
    // CU-27 - Seguir usuario
    // =========================================================
    @PostMapping("/{userId}/follow")
    public ResponseEntity<Follow> followUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID authenticatedUserId =
                AuthenticatedUser.getUserId(jwt);

        Follow follow = followService.followUser(
                authenticatedUserId,
                userId
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(follow);
    }


    // =========================================================
    // CU-28 - Dejar de seguir usuario
    // =========================================================
    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<Void> unfollowUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID authenticatedUserId =
                AuthenticatedUser.getUserId(jwt);

        followService.unfollowUser(
                authenticatedUserId,
                userId
        );

        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // Eliminar un seguidor
    // =========================================================
    @DeleteMapping("/followers/{followerId}")
    public ResponseEntity<Void> removeFollower(
            @PathVariable UUID followerId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID authenticatedUserId =
                AuthenticatedUser.getUserId(jwt);

        followService.removeFollower(
                authenticatedUserId,
                followerId
        );

        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // CU-30 - Consultar seguidores
    // =========================================================
    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<Follow>> getFollowers(
            @PathVariable UUID userId
    ) {

        return ResponseEntity.ok(
                followService.getFollowers(userId)
        );
    }


    // =========================================================
    // CU-30 - Consultar seguidos
    // =========================================================
    @GetMapping("/{userId}/following")
    public ResponseEntity<List<Follow>> getFollowing(
            @PathVariable UUID userId
    ) {

        return ResponseEntity.ok(
                followService.getFollowing(userId)
        );
    }
}