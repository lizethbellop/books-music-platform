package com.musa.profile.controller;

import com.musa.profile.dto.CreateUserListRequest;
import com.musa.profile.dto.UserListResponse;
import com.musa.profile.service.UserListService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles/me/lists")
public class UserListController {

    private final UserListService userListService;

    public UserListController(UserListService userListService) {
        this.userListService = userListService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserListResponse createList(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateUserListRequest request
    ) {
        return userListService.createList(userId, request);
    }

    @GetMapping
    public List<UserListResponse> getOwnLists(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return userListService.getOwnLists(userId);
    }
}