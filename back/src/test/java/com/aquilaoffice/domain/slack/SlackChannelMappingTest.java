package com.aquilaoffice.domain.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Slack channel mapping 도메인이 team/channel 정규화와 active 상태 전이를 책임지는지 검증한다.
 */
class SlackChannelMappingTest {

    @Test
    void createNormalizesSlackChannelAndStartsActive() {
        SlackChannelMapping mapping = SlackChannelMapping.create(
                101L,
                " T123 ",
                " C456 ",
                " dev-alerts ",
                " google-1 ");

        assertThat(mapping.getWorkspaceId()).isEqualTo(101L);
        assertThat(mapping.getTeamId()).isEqualTo("T123");
        assertThat(mapping.getChannelId()).isEqualTo("C456");
        assertThat(mapping.getChannelName()).isEqualTo("dev-alerts");
        assertThat(mapping.getCreatedByGoogleSub()).isEqualTo("google-1");
        assertThat(mapping.isActive()).isTrue();
        assertThat(mapping.hasSameChannel("T123", "C456")).isTrue();
    }

    @Test
    void restoreKeepsIdentifierAndInactiveStatus() {
        SlackChannelMapping mapping = SlackChannelMapping.restore(
                501L,
                101L,
                "T123",
                "C456",
                "dev-alerts",
                "google-1",
                false);

        assertThat(mapping.getId()).isEqualTo(501L);
        assertThat(mapping.isActive()).isFalse();
    }

    @Test
    void deactivateTurnsActiveMappingInactive() {
        SlackChannelMapping mapping = SlackChannelMapping.create(
                101L,
                "T123",
                "C456",
                null,
                "google-1");

        mapping.deactivate();

        assertThat(mapping.isActive()).isFalse();
    }

    @Test
    void hasSameChannelComparesNormalizedTeamAndChannel() {
        SlackChannelMapping mapping = SlackChannelMapping.create(
                101L,
                " T123 ",
                " C456 ",
                null,
                "google-1");

        assertThat(mapping.hasSameChannel(" T123 ", " C456 ")).isTrue();
        assertThat(mapping.hasSameChannel("T123", "C999")).isFalse();
    }

    @Test
    void createRejectsInvalidRequiredValues() {
        assertThatThrownBy(() -> SlackChannelMapping.create(0L, "T123", "C456", null, "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must be positive");
        assertThatThrownBy(() -> SlackChannelMapping.create(101L, " ", "C456", null, "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("teamId must not be blank");
        assertThatThrownBy(() -> SlackChannelMapping.create(101L, "T123", " ", null, "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("channelId must not be blank");
        assertThatThrownBy(() -> SlackChannelMapping.create(101L, "T123", "C456", null, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("createdByGoogleSub must not be blank");
    }
}
