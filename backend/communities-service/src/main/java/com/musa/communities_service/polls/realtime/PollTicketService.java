package com.musa.communities_service.polls.realtime;

import com.musa.communities_service.polls.exception.PollCapacityException;
import com.musa.communities_service.polls.service.PollService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

@Service
public class PollTicketService {

    public record Identity(
            UUID userId,
            UUID pollId,
            UUID communityId,
            Instant expiresAt,
            Instant ticketExpiresAt
    ) {
    }

    public record Response(
            String ticket,
            Instant expiresAt
    ) {
    }

    private final HashMap<String, Identity> tickets = new HashMap<>();
    private final PollService polls;

    public PollTicketService(PollService polls) {
        this.polls = polls;
    }

    public Response issue(
            UUID communityId,
            UUID pollId,
            Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        // Comprueba que el usuario pueda consultar esta encuesta.
        polls.get(communityId, pollId, userId);

        Instant now = Instant.now();

        if (jwt.getExpiresAt() == null
                || !jwt.getExpiresAt().isAfter(now)) {
            throw new PollCapacityException(
                    "La sesión venció. Inicia sesión nuevamente."
            );
        }

        synchronized (this) {
            cleanup();

            long userTickets = tickets.values().stream()
                    .filter(ticket -> ticket.userId().equals(userId))
                    .count();

            if (tickets.size() >= 1000 || userTickets >= 3) {
                throw new PollCapacityException(
                        "Espera unos segundos antes de abrir otra conexión."
                );
            }

            String key = UUID.randomUUID().toString();

            Instant sessionLimit = now.plusSeconds(300);
            Instant sessionExpires =
                    jwt.getExpiresAt().isBefore(sessionLimit)
                            ? jwt.getExpiresAt()
                            : sessionLimit;

            Instant ticketLimit = now.plusSeconds(30);
            Instant ticketExpires =
                    sessionExpires.isBefore(ticketLimit)
                            ? sessionExpires
                            : ticketLimit;

            tickets.put(key, new Identity(
                    userId,
                    pollId,
                    communityId,
                    sessionExpires,
                    ticketExpires
            ));

            return new Response(key, ticketExpires);
        }
    }

    public synchronized Identity consume(String key) {
        Identity identity = tickets.remove(key);

        if (identity == null
                || !identity.ticketExpiresAt().isAfter(Instant.now())) {
            return null;
        }

        return identity;
    }

    @Scheduled(
            fixedDelay = 10000,
            scheduler = "pollMaintenanceScheduler"
    )
    public synchronized void cleanup() {
        Instant now = Instant.now();

        tickets.values().removeIf(
                ticket -> !ticket.ticketExpiresAt().isAfter(now)
        );
    }
}