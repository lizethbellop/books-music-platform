package com.musa.communities_service.polls.controller;

import com.musa.communities_service.polls.exception.InvalidPollRequestException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/polls/communities")
public class PollCommunityController {

    public record CommunityItem(
            UUID id,
            String name,
            String role
    ) {
    }

    public record CommunityPage(
            List<CommunityItem> items,
            boolean hasMore,
            int nextPage
    ) {
    }

    private final JdbcTemplate jdbc;

    public PollCommunityController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    @Transactional(readOnly = true, timeout = 4)
    public CommunityPage list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page
    ) {
        if (page < 0 || page > 1000) {
            throw new InvalidPollRequestException("Página inválida");
        }

        var rows = jdbc.query("""
                SELECT c.id, c.name, m.role
                FROM communities c
                JOIN community_members m
                    ON m.community_id = c.id
                WHERE m.user_id = ?
                  AND m.status = 'ACTIVO'
                  AND c.status = 'ACTIVA'
                ORDER BY c.name, c.id
                LIMIT 21 OFFSET ?
                """,
                (row, index) -> new CommunityItem(
                        row.getObject(1, UUID.class),
                        row.getString(2),
                        row.getString(3)
                ),
                UUID.fromString(jwt.getSubject()),
                page * 20
        );

        return new CommunityPage(
                List.copyOf(
                        rows.subList(0, Math.min(20, rows.size()))
                ),
                rows.size() > 20,
                page + 1
        );
    }
}