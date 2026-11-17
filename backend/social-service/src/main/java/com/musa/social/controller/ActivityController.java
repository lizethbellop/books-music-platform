package com.musa.social.controller;

import com.musa.social.dto.response.PostResponse;
import com.musa.social.service.PostService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/social/users")
@RequiredArgsConstructor
public class ActivityController {

    private final PostService postService;

    // Consultar actividad de usuario
    @GetMapping("/{userId}/activity")
    public ResponseEntity<List<PostResponse>> getUserActivity(
            @PathVariable UUID userId
    ) {

        return ResponseEntity.ok(
                postService.getUserActivity(userId)
        );
    }
}