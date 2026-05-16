package com.aquilaoffice.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ChatSessionTest {

    @Test
    void createNormalizesRequesterGoogleSub() {
        ChatSession session = ChatSession.create(101L, " google-1 ");

        assertThat(session.getId()).isNull();
        assertThat(session.getWorkspaceId()).isEqualTo(101L);
        assertThat(session.getCreatedByGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void restoreKeepsSavedValues() {
        ChatSession session = ChatSession.restore(901L, 101L, "google-1");

        assertThat(session.getId()).isEqualTo(901L);
        assertThat(session.getWorkspaceId()).isEqualTo(101L);
        assertThat(session.getCreatedByGoogleSub()).isEqualTo("google-1");
    }

    @Test
    void createRejectsMissingRequiredValues() {
        assertThatThrownBy(() -> ChatSession.create(null, "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must not be null");
        assertThatThrownBy(() -> ChatSession.create(101L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("createdByGoogleSub must not be blank");
    }
}
