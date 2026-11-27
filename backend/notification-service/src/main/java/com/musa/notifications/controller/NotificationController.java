package com.musa.notifications.controller;
import com.musa.notifications.dto.NotificationPage;
import com.musa.notifications.service.NotificationInboxService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationInboxService service;
    public NotificationController(NotificationInboxService service) { this.service = service; }
    @GetMapping
    public NotificationPage list(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return service.list(UUID.fromString(jwt.getSubject()), page, size, unreadOnly);
    }
    @GetMapping("/unread-count")
    public Map<String, Long> unread(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("count", service.unreadCount(UUID.fromString(jwt.getSubject())));
    }
    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.markRead(UUID.fromString(jwt.getSubject()), id);
    }
    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("updated", service.markAllRead(UUID.fromString(jwt.getSubject())));
    }
}
