package com.musa.social.controller;

import com.musa.social.config.AuthenticatedUser;
import com.musa.social.dto.response.FeedPageResponse;
import com.musa.social.service.FeedService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;


    // =========================================================
    // Consultar feed social paginado
    // =========================================================
    @GetMapping
    public ResponseEntity<FeedPageResponse> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID authenticatedUserId =
                AuthenticatedUser.getUserId(jwt);

        return ResponseEntity.ok(
                feedService.getFeed(
                        authenticatedUserId,
                        page,
                        size
                )
        );
    }
}