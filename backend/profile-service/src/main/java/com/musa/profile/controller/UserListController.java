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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

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
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateUserListRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return userListService.createList(userId, request);
    }

    @GetMapping
    public List<UserListResponse> getOwnLists(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return userListService.getOwnLists(userId);
    }

    @PutMapping("/{listId}")
    public UserListResponse updateList(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID listId,
            @Valid @RequestBody UpdateUserListRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return userListService.updateList(userId, listId, request);
    }

    @PostMapping("/{listId}/elements")
    @ResponseStatus(HttpStatus.CREATED)
    public ListElementResponse addElement(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID listId,
            @Valid @RequestBody AddListElementRequest request
    ){
        UUID userId = UUID.fromString(jwt.getSubject());
        return userListService.addElement(
                userId,
                listId,
                request,
                jwt.getTokenValue()
        );
    }

    @GetMapping("/{listId}")
    public UserListDetailResponse getListDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID listId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return userListService.getListDetail(
                userId,
                listId,
                jwt.getTokenValue()
        );
    }

    @DeleteMapping("/{listId}/elements/{elementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeElement(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID listId,
            @PathVariable UUID elementId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        userListService.removeElement(userId, listId, elementId);
    }

    @DeleteMapping("/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteList(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID listId
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        userListService.deleteList(userId, listId);
    }
}