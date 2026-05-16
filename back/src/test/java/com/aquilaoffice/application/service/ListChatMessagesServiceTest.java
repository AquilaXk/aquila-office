package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListChatSessionMessagesQuery;
import com.aquilaoffice.application.port.in.ListChatSessionMessagesResult;
import com.aquilaoffice.application.port.in.ListChatTaskMessagesQuery;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.out.LoadChatMessagesPort;
import com.aquilaoffice.application.port.out.LoadChatSessionPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SavedChatMessage;
import com.aquilaoffice.application.port.out.SavedChatSession;
import com.aquilaoffice.domain.chat.ChatMessageRole;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ListChatMessagesServiceTest {

    @Test
    void listTaskMessagesRequiresMembershipAndReturnsMessages() {
        ListChatTaskMessagesService service = new ListChatTaskMessagesService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "user@example.com", "User"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail())),
                new FakeLoadChatMessagesPort(List.of(savedMessage(1001L), savedMessage(1002L))));

        var results = service.listChatTaskMessages(new ListChatTaskMessagesQuery(101L, 301L, " google-1 "));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).messageId()).isEqualTo(1001L);
        assertThat(results.get(0).role()).isEqualTo(ChatMessageRole.USER);
    }

    @Test
    void listSessionMessagesUsesCursorAndHasMore() {
        ListChatSessionMessagesService service = new ListChatSessionMessagesService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "user@example.com", "User"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail())),
                new FakeLoadChatMessagesPort(List.of(savedMessage(1002L), savedMessage(1003L), savedMessage(1004L))),
                new FakeLoadChatSessionPort(Optional.of(savedSession(901L))));

        ListChatSessionMessagesResult result = service.listChatSessionMessages(
                new ListChatSessionMessagesQuery(101L, 901L, " google-1 ", 1001L, 2));

        assertThat(result.chatSessionId()).isEqualTo(901L);
        assertThat(result.messages()).hasSize(2);
        assertThat(result.nextCursor()).isEqualTo(1003L);
        assertThat(result.hasMore()).isTrue();
    }

    @Test
    void listSessionMessagesDefaultsInvalidLimit() {
        ListChatSessionMessagesService service = new ListChatSessionMessagesService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "user@example.com", "User"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail())),
                new FakeLoadChatMessagesPort(List.of(savedMessage(1002L))),
                new FakeLoadChatSessionPort(Optional.of(savedSession(901L))));

        ListChatSessionMessagesResult result = service.listChatSessionMessages(
                new ListChatSessionMessagesQuery(101L, 901L, "google-1", null, 0));

        assertThat(result.messages()).singleElement().satisfies(message -> assertThat(message.messageId()).isEqualTo(1002L));
        assertThat(result.nextCursor()).isNull();
        assertThat(result.hasMore()).isFalse();
    }

    private static GetWorkspaceDetailResult workspaceDetail() {
        return new GetWorkspaceDetailResult(
                101L,
                "Team",
                "Description",
                "google-1",
                WorkspaceMemberRole.MEMBER,
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static SavedChatSession savedSession(Long chatSessionId) {
        return new SavedChatSession(
                chatSessionId,
                101L,
                "google-1",
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static SavedChatMessage savedMessage(Long messageId) {
        return new SavedChatMessage(
                messageId,
                101L,
                901L,
                301L,
                701L,
                ChatMessageRole.USER,
                "message-" + messageId,
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static class FakeLoadMemberPort implements LoadMemberPort {
        private final Optional<Member> member;

        private FakeLoadMemberPort(Optional<Member> member) {
            this.member = member;
        }

        @Override
        public Optional<Member> findByGoogleSub(String googleSub) {
            return member;
        }
    }

    private static class FakeLoadWorkspacePort implements LoadWorkspacePort {
        private final Optional<GetWorkspaceDetailResult> detail;

        private FakeLoadWorkspacePort(Optional<GetWorkspaceDetailResult> detail) {
            this.detail = detail;
        }

        @Override
        public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            throw new UnsupportedOperationException("not used in chat list service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId,
                String googleSub) {
            return detail;
        }
    }

    private static class FakeLoadChatMessagesPort implements LoadChatMessagesPort {
        private final List<SavedChatMessage> messages;

        private FakeLoadChatMessagesPort(List<SavedChatMessage> messages) {
            this.messages = messages;
        }

        @Override
        public List<SavedChatMessage> findByWorkspaceIdAndTaskId(Long workspaceId, Long taskId) {
            return messages;
        }

        @Override
        public List<SavedChatMessage> findByWorkspaceIdAndChatSessionIdAfter(
                Long workspaceId,
                Long chatSessionId,
                Long afterMessageId,
                int limit) {
            return messages;
        }
    }

    private static class FakeLoadChatSessionPort implements LoadChatSessionPort {
        private final Optional<SavedChatSession> session;

        private FakeLoadChatSessionPort(Optional<SavedChatSession> session) {
            this.session = session;
        }

        @Override
        public Optional<SavedChatSession> findByWorkspaceIdAndChatSessionId(Long workspaceId, Long chatSessionId) {
            return session;
        }
    }
}
