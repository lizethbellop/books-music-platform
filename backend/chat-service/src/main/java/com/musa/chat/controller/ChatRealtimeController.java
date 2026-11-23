package com.musa.chat.controller;
import com.musa.chat.client.UsersClient;
import com.musa.chat.realtime.ChatTicketService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/chat/realtime")
public class ChatRealtimeController {
    private final ChatTicketService tickets;
    private final UsersClient users;
    public ChatRealtimeController(ChatTicketService tickets, UsersClient users) { this.tickets = tickets; this.users = users; }
    @PostMapping("/tickets")
    public ChatTicketService.TicketResponse ticket(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        users.requireActiveParticipants(userId, userId);
        return tickets.issue(jwt);
    }
}
