package com.musa.communities_service.polls.controller;

import com.musa.communities_service.polls.dto.CreatePollRequest;
import com.musa.communities_service.polls.dto.PollResponse;
import com.musa.communities_service.polls.dto.PollResultsResponse;
import com.musa.communities_service.polls.dto.VoteRequest;
import com.musa.communities_service.polls.service.PollService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.musa.communities_service.polls.realtime.PollTicketService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/polls")
public class PollController {

    private final PollService service;
    private final PollTicketService tickets;

    public PollController(
            PollService service,
            PollTicketService tickets
    ) {
        this.service = service;
        this.tickets = tickets;
    }

    @PostMapping
    public ResponseEntity<PollResponse> create(
            @PathVariable UUID communityId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePollRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                service.create(communityId, userId(jwt), request)
        );
    }

    @GetMapping
    public Slice<PollResponse> list(
            @PathVariable UUID communityId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(communityId, userId(jwt), page, size);
    }

    @GetMapping("/{pollId}")
    public PollResponse get(
            @PathVariable UUID communityId,
            @PathVariable UUID pollId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.get(communityId, pollId, userId(jwt));
    }

    @GetMapping("/{pollId}/results")
    public PollResultsResponse results(
            @PathVariable UUID communityId,
            @PathVariable UUID pollId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.results(communityId, pollId, userId(jwt));
    }

    @PostMapping("/{pollId}/votes")
    public PollResultsResponse vote(
            @PathVariable UUID communityId,
            @PathVariable UUID pollId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VoteRequest request
    ) {
        return service.vote(
                communityId, pollId, userId(jwt), request
        );
    }

    @PostMapping("/{pollId}/close")
    public PollResponse close(
            @PathVariable UUID communityId,
            @PathVariable UUID pollId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.close(communityId, pollId, userId(jwt));
    }

    @PostMapping("/{pollId}/realtime/tickets")
    public PollTicketService.Response ticket(
            @PathVariable UUID communityId,
            @PathVariable UUID pollId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return tickets.issue(communityId, pollId, jwt);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}