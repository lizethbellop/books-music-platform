package com.musa.profile.controller;

import com.musa.profile.dto.CreateUserListRequest;
import com.musa.profile.dto.UserListResponse;
import com.musa.profile.service.UserListService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.musa.profile.dto.UpdateUserListRequest;
import com.musa.profile.dto.AddListElementRequest;
import com.musa.profile.dto.ListElementResponse;
import com.musa.profile.dto.UserListDetailResponse;

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

    @PutMapping("/{listId}")
    public UserListResponse updateList(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID listId,
            @Valid @RequestBody UpdateUserListRequest request
    ) {
        return userListService.updateList(userId, listId, request);
    }

    @PostMapping("/{listId}/elements")
    @ResponseStatus(HttpStatus.CREATED)
    public ListElementResponse addElement(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID listId,
            @Valid @RequestBody AddListElementRequest request
    ){
        return userListService.addElement(userId, listId, request);
    }

    @GetMapping("/{listId}")
    public UserListDetailResponse getListDetail(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID listId
    ) {
        return userListService.getListDetail(userId, listId);
    }

    @DeleteMapping("/{listId}/elements/{elementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeElement(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID listId,
            @PathVariable UUID elementId
    ) {
        userListService.removeElement(userId, listId, elementId);
    }

    @DeleteMapping("/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteList(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID listId
    ) {
        userListService.deleteList(userId, listId);
    }
}