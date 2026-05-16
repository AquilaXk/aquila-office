package com.aquilaoffice.domain.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SlackOAuthStateTest {

    @Test
    void parseRestoresWorkspaceIdAndRequester() {
        SlackOAuthState state = SlackOAuthState.parse(" workspaceId:101:requester:google-1 ");

        assertThat(state.workspaceId()).isEqualTo(101L);
        assertThat(state.requesterGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void serializeUsesInstallUrlStateFormat() {
        String state = SlackOAuthState.serialize(101L, " google-1 ");

        assertThat(state).isEqualTo("workspaceId:101:requester:google-1");
    }

    @Test
    void parseRejectsInvalidState() {
        assertThatThrownBy(() -> SlackOAuthState.parse("workspaceId:abc:requester:google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slack oauth state workspaceId must be positive");
        assertThatThrownBy(() -> SlackOAuthState.parse("workspaceId:101:requester: "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slack oauth state requesterGoogleSub must not be blank");
        assertThatThrownBy(() -> SlackOAuthState.parse("workspace:101:google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slack oauth state is invalid");
    }
}
