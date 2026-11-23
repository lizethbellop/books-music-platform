package com.musa.chat.realtime;
import com.musa.chat.exception.InvalidChatRequestException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatTicketService {
    public record TicketIdentity(UUID userId, Instant sessionExpiresAt, Instant ticketExpiresAt) {}
    public record TicketResponse(String ticket, Instant expiresAt) {}
    private final ConcurrentHashMap<String, TicketIdentity> tickets = new ConcurrentHashMap<>();

    public synchronized TicketResponse issue(Jwt jwt) {
        cleanup();
        if (tickets.size() >= 10000) throw new InvalidChatRequestException("Inténtalo de nuevo en unos segundos.");
        Instant now = Instant.now();
        if (jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(now))
            throw new InvalidChatRequestException("La sesión venció.");
        String key = UUID.randomUUID().toString();
        Instant expires = now.plusSeconds(30);
        if (expires.isAfter(jwt.getExpiresAt())) expires = jwt.getExpiresAt();
        tickets.put(key, new TicketIdentity(UUID.fromString(jwt.getSubject()), jwt.getExpiresAt(), expires));
        return new TicketResponse(key, expires);
    }

    public TicketIdentity consume(String key) {
        if (key == null) return null;
        TicketIdentity identity = tickets.remove(key);
        return identity != null && identity.ticketExpiresAt().isAfter(Instant.now()) ? identity : null;
    }

    @Scheduled(fixedDelay = 10000)
    public void cleanup() {
        Instant now = Instant.now();
        tickets.entrySet().removeIf(e -> !e.getValue().ticketExpiresAt().isAfter(now));
    }
}
