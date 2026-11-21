package com.aquilaoffice.adapter.in.web.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.domain.slack.SlackOAuthState;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SlackInstallUrlBuilderTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-20T00:00:00Z"), ZoneOffset.UTC);
    private static final String STATE_SECRET = "test-slack-oauth-state-secret-0123456789abcdef";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    @Test
    void buildReturnsSlackAuthorizeUrlWithWorkspaceState() {
        SlackInstallUrlBuilder builder = new SlackInstallUrlBuilder(
                " client-123 ",
                " http://localhost:8080/api/v1/slack/oauth/callback ",
                URI.create("https://slack.com/oauth/v2/authorize"),
                CLOCK,
                STATE_SECRET,
                STATE_TTL);

        String url = builder.build(101L, " google-1 ");

        assertThat(url).startsWith("https://slack.com/oauth/v2/authorize?");
        assertThat(url).contains("client_id=client-123");
        assertThat(url).contains("redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fapi%2Fv1%2Fslack%2Foauth%2Fcallback");
        assertThat(url).contains(
                "scope=app_mentions%3Aread%2Cchannels%3Aread%2Cchat%3Awrite%2Cincoming-webhook");
        SlackOAuthState state = SlackOAuthState.parse(extractState(url), STATE_SECRET, CLOCK.instant().plusSeconds(30));
        assertThat(state.workspaceId()).isEqualTo(101L);
        assertThat(state.requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void buildUsesConfiguredScopes() {
        SlackInstallUrlBuilder builder = new SlackInstallUrlBuilder(
                "client-123",
                "http://localhost:8080/api/v1/slack/oauth/callback",
                URI.create("https://slack.com/oauth/v2/authorize"),
                "app_mentions:read,chat:write,commands",
                CLOCK,
                STATE_SECRET,
                STATE_TTL);

        String url = builder.build(101L, "google-1");

        assertThat(url).contains("scope=app_mentions%3Aread%2Cchat%3Awrite%2Ccommands");
        assertThat(url).doesNotContain("incoming-webhook");
    }

    @Test
    void buildFailsWhenClientIdIsMissing() {
        SlackInstallUrlBuilder builder = new SlackInstallUrlBuilder(
                " ",
                "http://localhost:8080/api/v1/slack/oauth/callback",
                URI.create("https://slack.com/oauth/v2/authorize"),
                CLOCK,
                STATE_SECRET,
                STATE_TTL);

        assertThatThrownBy(() -> builder.build(101L, "google-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("slack client id is not configured");
    }

    private static String extractState(String url) {
        String encodedState = url.substring(url.indexOf("state=") + "state=".length());
        return URLDecoder.decode(encodedState, StandardCharsets.UTF_8);
    }
}
