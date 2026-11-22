package com.aquilaoffice.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PresenceStreamTicketServiceTest {

    @Test
    void issuesSingleUseWorkspaceScopedTicketAndRejectsReplayExpiryOrWorkspaceMismatch() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-22T00:00:00Z"));
        Clock clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneId.of("UTC");
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };
        PresenceStreamTicketService service = new PresenceStreamTicketService(clock, Duration.ofSeconds(30), 4);

        PresenceStreamTicketService.IssuedTicket issued = service.issueTicket(10L, "sub-1");
        assertThat(issued.expiresInSeconds()).isEqualTo(30);
        assertThat(issued.ticket()).isNotBlank();

        // 다른 workspace에는 소비 불가
        assertThat(service.consumeTicket(issued.ticket(), 99L)).isEmpty();

        // 만료 후 소비 불가
        PresenceStreamTicketService.IssuedTicket expiring = service.issueTicket(10L, "sub-2");
        now.set(now.get().plusSeconds(31));
        assertThat(service.consumeTicket(expiring.ticket(), 10L)).isEmpty();

        // 일회성 소비 성공 후 재사용 불가
        PresenceStreamTicketService.IssuedTicket fresh = service.issueTicket(10L, "sub-3");
        assertThat(service.consumeTicket(fresh.ticket(), 10L))
                .contains(new PresenceStreamTicketService.PresenceTicketPrincipal(10L, "sub-3"));
        assertThat(service.consumeTicket(fresh.ticket(), 10L)).isEmpty();
    }
}
