package com.aquilaoffice.global.security;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * EventSource URL에 장기 access token을 노출하지 않기 위한 일회성 단기(30초) SSE 스트림 티켓 관리자다.
 */
@Service
public class PresenceStreamTicketService {

    public static final int DEFAULT_TTL_SECONDS = 30;
    private static final int TICKET_RANDOM_BYTES = 32;
    private static final int DEFAULT_MAX_ACTIVE_TICKETS = 8192;

    private final ConcurrentHashMap<String, TicketEntry> tickets = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom;
    private final Clock clock;
    private final Duration ttl;
    private final int maxActiveTickets;

    @Autowired
    public PresenceStreamTicketService() {
        this(Clock.systemUTC(), Duration.ofSeconds(DEFAULT_TTL_SECONDS), DEFAULT_MAX_ACTIVE_TICKETS);
    }

    PresenceStreamTicketService(Clock clock, Duration ttl, int maxActiveTickets) {
        this.secureRandom = new SecureRandom();
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.ttl = Objects.requireNonNull(ttl, "ttl must not be null");
        if (maxActiveTickets <= 0) {
            throw new IllegalArgumentException("maxActiveTickets must be positive");
        }
        this.maxActiveTickets = maxActiveTickets;
    }

    public IssuedTicket issueTicket(Long workspaceId, String memberGoogleSub) {
        if (workspaceId == null || workspaceId <= 0L) {
            throw new IllegalArgumentException("workspaceId must be positive");
        }
        if (memberGoogleSub == null || memberGoogleSub.isBlank()) {
            throw new IllegalArgumentException("memberGoogleSub must not be blank");
        }
        Instant now = clock.instant();
        evictExpired(now);
        if (tickets.size() >= maxActiveTickets) {
            evictOneOldest();
        }
        byte[] randomBytes = new byte[TICKET_RANDOM_BYTES];
        secureRandom.nextBytes(randomBytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant expiresAt = now.plus(ttl);
        tickets.put(ticket, new TicketEntry(workspaceId, memberGoogleSub.trim(), expiresAt));
        return new IssuedTicket(ticket, (int) ttl.toSeconds());
    }

    public Optional<PresenceTicketPrincipal> consumeTicket(String ticket, Long expectedWorkspaceId) {
        if (ticket == null || ticket.isBlank() || expectedWorkspaceId == null) {
            return Optional.empty();
        }
        TicketEntry entry = tickets.remove(ticket.trim());
        if (entry == null) {
            return Optional.empty();
        }
        if (clock.instant().isAfter(entry.expiresAt())) {
            return Optional.empty();
        }
        if (!Objects.equals(entry.workspaceId(), expectedWorkspaceId)) {
            return Optional.empty();
        }
        return Optional.of(new PresenceTicketPrincipal(entry.workspaceId(), entry.memberGoogleSub()));
    }

    private void evictExpired(Instant now) {
        if (tickets.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, TicketEntry>> iterator = tickets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, TicketEntry> entry = iterator.next();
            if (now.isAfter(entry.getValue().expiresAt())) {
                iterator.remove();
            }
        }
    }

    private void evictOneOldest() {
        String oldestKey = null;
        Instant oldestExpiry = null;
        for (Map.Entry<String, TicketEntry> entry : tickets.entrySet()) {
            Instant expiry = entry.getValue().expiresAt();
            if (oldestExpiry == null || expiry.isBefore(oldestExpiry)) {
                oldestExpiry = expiry;
                oldestKey = entry.getKey();
            }
        }
        if (oldestKey != null) {
            tickets.remove(oldestKey);
        }
    }

    public record IssuedTicket(String ticket, int expiresInSeconds) {}

    public record PresenceTicketPrincipal(Long workspaceId, String memberGoogleSub) {}

    private record TicketEntry(Long workspaceId, String memberGoogleSub, Instant expiresAt) {}
}
