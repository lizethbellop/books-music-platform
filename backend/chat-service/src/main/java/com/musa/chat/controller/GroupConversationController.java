package com.musa.chat.controller;
import com.musa.chat.dto.request.*;
import com.musa.chat.dto.response.ConversationResponse;
import com.musa.chat.service.GroupConversationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/chat/conversations")
public class GroupConversationController {
    private final GroupConversationService service;
    public GroupConversationController(GroupConversationService service) { this.service = service; }
    private UUID user(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    @PostMapping("/groups") public ConversationResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateGroupRequest request) { return service.create(user(jwt), request); }
    @GetMapping("/{id}") public ConversationResponse details(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return service.details(user(jwt), id); }
    @PatchMapping("/{id}/group") public ConversationResponse rename(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody GroupNameRequest request) { return service.rename(user(jwt), id, request.name()); }
    @PostMapping("/{id}/participants") public ConversationResponse add(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody GroupUserRequest request) { return service.add(user(jwt), id, request.userId()); }
    @DeleteMapping("/{id}/participants/{memberId}") public ConversationResponse remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID memberId) { return service.remove(user(jwt), id, memberId); }
    @PostMapping("/{id}/admin") public ConversationResponse transfer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody GroupUserRequest request) { return service.transfer(user(jwt), id, request.userId()); }
    @PostMapping("/{id}/leave") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { service.leave(user(jwt), id); }
}
