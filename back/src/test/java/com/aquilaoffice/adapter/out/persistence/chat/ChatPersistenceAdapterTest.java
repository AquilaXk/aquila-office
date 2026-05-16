package com.aquilaoffice.adapter.out.persistence.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.LoadChatMessagesPort;
import com.aquilaoffice.application.port.out.LoadChatSessionPort;
import com.aquilaoffice.application.port.out.SaveChatMessagePort;
import com.aquilaoffice.application.port.out.SaveChatSessionPort;
import com.aquilaoffice.application.port.out.SavedChatSession;
import com.aquilaoffice.domain.chat.ChatMessage;
import com.aquilaoffice.domain.chat.ChatMessageRole;
import com.aquilaoffice.domain.chat.ChatSession;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(ChatPersistenceAdapter.class)
class ChatPersistenceAdapterTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private SaveChatSessionPort saveChatSessionPort;

    @Autowired
    private LoadChatSessionPort loadChatSessionPort;

    @Autowired
    private SaveChatMessagePort saveChatMessagePort;

    @Autowired
    private LoadChatMessagesPort loadChatMessagesPort;

    @Test
    void saveSessionAndMessagesThenListsByTaskAndCursor() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("google-1");
        SavedChatSession session = saveChatSessionPort.save(ChatSession.create(workspaceEntity.getId(), "google-1"));

        var first = saveChatMessagePort.save(ChatMessage.create(
                workspaceEntity.getId(),
                session.chatSessionId(),
                301L,
                null,
                ChatMessageRole.USER,
                "작업해 주세요."));
        var second = saveChatMessagePort.save(ChatMessage.create(
                workspaceEntity.getId(),
                session.chatSessionId(),
                301L,
                701L,
                ChatMessageRole.ASSISTANT,
                "완료했습니다."));
        var third = saveChatMessagePort.save(ChatMessage.create(
                workspaceEntity.getId(),
                session.chatSessionId(),
                302L,
                null,
                ChatMessageRole.USER,
                "다음 작업"));

        assertThat(loadChatSessionPort.findByWorkspaceIdAndChatSessionId(
                        workspaceEntity.getId(),
                        session.chatSessionId()))
                .hasValueSatisfying(found -> assertThat(found.createdByGoogleSub()).isEqualTo("google-1"));
        assertThat(loadChatMessagesPort.findByWorkspaceIdAndTaskId(workspaceEntity.getId(), 301L))
                .extracting("messageId")
                .containsExactly(first.messageId(), second.messageId());
        assertThat(loadChatMessagesPort.findByWorkspaceIdAndChatSessionIdAfter(
                        workspaceEntity.getId(),
                        session.chatSessionId(),
                        first.messageId(),
                        2))
                .extracting("messageId")
                .containsExactly(second.messageId(), third.messageId());
    }

    @Test
    void saveSessionRejectsMissingWorkspaceEntity() {
        assertThatThrownBy(() -> saveChatSessionPort.save(ChatSession.create(404L, "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace entity not found");
    }

    @Test
    void saveMessageRejectsMissingSessionEntity() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("google-1");

        assertThatThrownBy(() -> saveChatMessagePort.save(ChatMessage.create(
                        workspaceEntity.getId(),
                        999L,
                        301L,
                        null,
                        ChatMessageRole.USER,
                        "작업해 주세요.")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("chat session entity not found");
    }

    private WorkspaceJpaEntity saveWorkspaceEntity(String googleSub) {
        MemberJpaEntity creatorEntity = memberJpaRepository.save(MemberJpaEntity.create(
                googleSub, googleSub + "@example.com", "Creator", MemberRole.USER));
        Workspace workspace =
                Workspace.create("Team", "Description", Member.createUser(googleSub, googleSub + "@example.com", "Creator"));
        return workspaceJpaRepository.save(WorkspaceJpaEntity.from(workspace, creatorEntity));
    }
}
