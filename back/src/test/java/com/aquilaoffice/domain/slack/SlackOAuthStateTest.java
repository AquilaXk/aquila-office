package com.aquilaoffice.domain.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SlackOAuthStateTest {

    private static final String STATE_SECRET = "test-slack-oauth-state-secret-0123456789abcdef";
    private static final Instant ISSUED_AT = Instant.parse("2026-05-20T00:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);

    @Test
    void parseRestoresSignedWorkspaceIdAndRequester() {
        String serializedState = SlackOAuthState.serialize(101L, " google-1 ", ISSUED_AT, TTL, STATE_SECRET);

        SlackOAuthState state = SlackOAuthState.parse(
                " " + serializedState + " ",
                STATE_SECRET,
                ISSUED_AT.plusSeconds(30));

        assertThat(state.workspaceId()).isEqualTo(101L);
        assertThat(state.requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void serializeUsesSignedStateFormat() {
        String state = SlackOAuthState.serialize(101L, " google-1 ", ISSUED_AT, TTL, STATE_SECRET);

        assertThat(state).startsWith("v1.101.");
        assertThat(state.split("\\.", -1)).hasSize(5);
    }

    @Test
    void parseRejectsTamperedState() {
        String state = SlackOAuthState.serialize(101L, "google-1", ISSUED_AT, TTL, STATE_SECRET);
        String tamperedState = state.replace("101", "102");

        assertThatThrownBy(() -> SlackOAuthState.parse(tamperedState, STATE_SECRET, ISSUED_AT.plusSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slack oauth state signature is invalid");
    }

    @Test
    void parseRejectsExpiredState() {
        String state = SlackOAuthState.serialize(101L, "google-1", ISSUED_AT, TTL, STATE_SECRET);

        assertThatThrownBy(() -> SlackOAuthState.parse(state, STATE_SECRET, ISSUED_AT.plus(TTL)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slack oauth state is expired");
    }

    @Test
    void parseRejectsInvalidState() {
        assertThatThrownBy(() -> SlackOAuthState.parse("workspace:101:google-1", STATE_SECRET, ISSUED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slack oauth state is invalid");
        assertThatThrownBy(() -> SlackOAuthState.serialize(101L, "google-1", ISSUED_AT, Duration.ZERO, STATE_SECRET))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slack oauth state ttl must be positive");
    }
}
