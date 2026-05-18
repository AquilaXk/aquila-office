package com.aquilaoffice.adapter.in.web.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;

class SlackInstallUrlBuilderTest {

    @Test
    void buildReturnsSlackAuthorizeUrlWithWorkspaceState() {
        SlackInstallUrlBuilder builder = new SlackInstallUrlBuilder(
                " client-123 ",
                " http://localhost:8080/api/v1/slack/oauth/callback ",
                URI.create("https://slack.com/oauth/v2/authorize"));

        String url = builder.build(101L, " google-1 ");

        assertThat(url).startsWith("https://slack.com/oauth/v2/authorize?");
        assertThat(url).contains("client_id=client-123");
        assertThat(url).contains("redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fapi%2Fv1%2Fslack%2Foauth%2Fcallback");
        assertThat(url).contains(
                "scope=app_mentions%3Aread%2Cchannels%3Aread%2Cchat%3Awrite%2Cincoming-webhook");
        assertThat(url).contains("state=workspaceId%3A101%3Arequester%3Agoogle-1");
    }

    @Test
    void buildUsesConfiguredScopes() {
        SlackInstallUrlBuilder builder = new SlackInstallUrlBuilder(
                "client-123",
                "http://localhost:8080/api/v1/slack/oauth/callback",
                URI.create("https://slack.com/oauth/v2/authorize"),
                "app_mentions:read,chat:write,commands");

        String url = builder.build(101L, "google-1");

        assertThat(url).contains("scope=app_mentions%3Aread%2Cchat%3Awrite%2Ccommands");
        assertThat(url).doesNotContain("incoming-webhook");
    }

    @Test
    void buildFailsWhenClientIdIsMissing() {
        SlackInstallUrlBuilder builder = new SlackInstallUrlBuilder(
                " ",
                "http://localhost:8080/api/v1/slack/oauth/callback",
                URI.create("https://slack.com/oauth/v2/authorize"));

        assertThatThrownBy(() -> builder.build(101L, "google-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("slack client id is not configured");
    }
}
