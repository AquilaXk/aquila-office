package com.aquilaoffice.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ChatMessageTest {

    @Test
    void createUserMessageNormalizesContentAndLinksTask() {
        ChatMessage message = ChatMessage.create(
                101L,
                901L,
                301L,
                null,
                ChatMessageRole.USER,
                " 기능을 구현해 주세요. ");

        assertThat(message.getId()).isNull();
        assertThat(message.getWorkspaceId()).isEqualTo(101L);
        assertThat(message.getChatSessionId()).isEqualTo(901L);
        assertThat(message.getTaskId()).isEqualTo(301L);
        assertThat(message.getTaskExecutionId()).isNull();
        assertThat(message.getRole()).isEqualTo(ChatMessageRole.USER);
        assertThat(message.getContent()).isEqualTo("기능을 구현해 주세요.");
    }

    @Test
    void createAssistantMessageCanLinkExecution() {
        ChatMessage message = ChatMessage.create(
                101L,
                901L,
                301L,
                701L,
                ChatMessageRole.ASSISTANT,
                " 완료했습니다. ");

        assertThat(message.getTaskExecutionId()).isEqualTo(701L);
        assertThat(message.getRole()).isEqualTo(ChatMessageRole.ASSISTANT);
        assertThat(message.getContent()).isEqualTo("완료했습니다.");
    }

    @Test
    void restoreKeepsSavedValues() {
        ChatMessage message = ChatMessage.restore(
                1001L,
                101L,
                901L,
                301L,
                701L,
                ChatMessageRole.SYSTEM,
                "system note");

        assertThat(message.getId()).isEqualTo(1001L);
        assertThat(message.getWorkspaceId()).isEqualTo(101L);
        assertThat(message.getChatSessionId()).isEqualTo(901L);
        assertThat(message.getTaskId()).isEqualTo(301L);
        assertThat(message.getTaskExecutionId()).isEqualTo(701L);
        assertThat(message.getRole()).isEqualTo(ChatMessageRole.SYSTEM);
        assertThat(message.getContent()).isEqualTo("system note");
    }

    @Test
    void createRejectsMissingRequiredValues() {
        assertThatThrownBy(() -> ChatMessage.create(null, 901L, 301L, null, ChatMessageRole.USER, "message"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must not be null");
        assertThatThrownBy(() -> ChatMessage.create(101L, null, 301L, null, ChatMessageRole.USER, "message"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("chatSessionId must not be null");
        assertThatThrownBy(() -> ChatMessage.create(101L, 901L, 301L, null, null, "message"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("role must not be null");
        assertThatThrownBy(() -> ChatMessage.create(101L, 901L, 301L, null, ChatMessageRole.USER, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("content must not be blank");
    }
}
