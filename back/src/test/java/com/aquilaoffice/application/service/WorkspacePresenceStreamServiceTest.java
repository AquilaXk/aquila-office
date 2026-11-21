package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.CloseWorkspacePresenceStreamCommand;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.OpenWorkspacePresenceStreamCommand;
import com.aquilaoffice.application.port.in.OpenWorkspacePresenceStreamResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import com.aquilaoffice.domain.workspace.WorkspacePresenceMessage;
import com.aquilaoffice.domain.workspace.WorkspacePresencePosition;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 presence stream service가 snapshot과 접속 이벤트를 sink에 전달하는지 확인한다.
 */
class WorkspacePresenceStreamServiceTest {

    @Test
    void openStreamSendsSnapshotAndRecordsConnection() {
        WorkspacePresenceRegistry registry = new WorkspacePresenceRegistry();
        registry.updatePosition(101L, 2L, new WorkspacePresencePosition("10px", "20px"), "online");
        OpenWorkspacePresenceStreamService service = service(
                Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN)),
                members(adminMember(1L), memberMember(2L)),
                registry);
        RecordingPresenceEventSink sink = new RecordingPresenceEventSink();

        OpenWorkspacePresenceStreamResult result =
                service.openStream(new OpenWorkspacePresenceStreamCommand(101L, " google-1 "), sink);

        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(registry.connectionsInWorkspace(101L)).hasSize(1);
        assertThat(sink.messages()).hasSize(1);
        WorkspacePresenceMessage snapshot = sink.messages().getFirst();
        assertThat(snapshot.type()).isEqualTo("presence.snapshot");
        assertThat(snapshot.workspaceId()).isEqualTo(101L);
        assertThat(snapshot.members()).extracting("memberId").containsExactly(1L, 2L);
    }

    @Test
    void openStreamBroadcastsJoinToExistingSinks() {
        WorkspacePresenceRegistry registry = new WorkspacePresenceRegistry();
        OpenWorkspacePresenceStreamService service = service(
                Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER)),
                members(adminMember(1L), memberMember(2L)),
                registry);
        RecordingPresenceEventSink firstSink = new RecordingPresenceEventSink();
        RecordingPresenceEventSink secondSink = new RecordingPresenceEventSink();

        service.openStream(new OpenWorkspacePresenceStreamCommand(101L, "google-1"), firstSink);
        service.openStream(new OpenWorkspacePresenceStreamCommand(101L, "google-2"), secondSink);

        assertThat(firstSink.messages())
                .extracting(WorkspacePresenceMessage::type)
                .contains("presence.join");
        WorkspacePresenceMessage join = firstSink.messages().getLast();
        assertThat(join.memberId()).isEqualTo(2L);
        assertThat(secondSink.messages().getFirst().type()).isEqualTo("presence.snapshot");
    }

    @Test
    void closeStreamBroadcastsLeaveToRemainingSinks() {
        WorkspacePresenceRegistry registry = new WorkspacePresenceRegistry();
        OpenWorkspacePresenceStreamService service = service(
                Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER)),
                members(adminMember(1L), memberMember(2L)),
                registry);
        RecordingPresenceEventSink firstSink = new RecordingPresenceEventSink();
        RecordingPresenceEventSink secondSink = new RecordingPresenceEventSink();
        service.openStream(new OpenWorkspacePresenceStreamCommand(101L, "google-1"), firstSink);
        OpenWorkspacePresenceStreamResult second =
                service.openStream(new OpenWorkspacePresenceStreamCommand(101L, "google-2"), secondSink);
        firstSink.clear();

        service.closeStream(new CloseWorkspacePresenceStreamCommand(
                second.workspaceId(), second.memberId(), secondSink));

        assertThat(registry.connectionsInWorkspace(101L)).hasSize(1);
        assertThat(firstSink.messages()).hasSize(1);
        WorkspacePresenceMessage leave = firstSink.messages().getFirst();
        assertThat(leave.type()).isEqualTo("presence.leave");
        assertThat(leave.memberId()).isEqualTo(2L);
    }

    @Test
    void openStreamRejectsNotJoinedWorkspace() {
        WorkspacePresenceRegistry registry = new WorkspacePresenceRegistry();
        OpenWorkspacePresenceStreamService service = service(
                Optional.empty(),
                members(memberMember(2L)),
                registry);

        assertThatThrownBy(() -> service.openStream(
                        new OpenWorkspacePresenceStreamCommand(101L, "google-2"),
                        new RecordingPresenceEventSink()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace not found");
        assertThat(registry.connectionsInWorkspace(101L)).isEmpty();
    }

    private OpenWorkspacePresenceStreamService service(
            Optional<GetWorkspaceDetailResult> workspaceDetail,
            List<ListWorkspaceMembersResult> members,
            WorkspacePresenceRegistry registry) {
        return new OpenWorkspacePresenceStreamService(
                new FakeLoadMemberPort(),
                new FakeLoadWorkspacePort(workspaceDetail),
                new FakeLoadWorkspaceMemberPort(members),
                registry);
    }

    private static GetWorkspaceDetailResult workspaceDetail(WorkspaceMemberRole role) {
        return new GetWorkspaceDetailResult(
                101L,
                "Team",
                "Description",
                "google-1",
                role,
                LocalDateTime.of(2026, 5, 15, 10, 0));
    }

    private static ListWorkspaceMembersResult adminMember(Long memberId) {
        return new ListWorkspaceMembersResult(
                memberId,
                "google-" + memberId,
                "admin" + memberId + "@example.com",
                "Admin",
                WorkspaceMemberRole.ADMIN,
                LocalDateTime.of(2026, 5, 15, 10, 0));
    }

    private static ListWorkspaceMembersResult memberMember(Long memberId) {
        return new ListWorkspaceMembersResult(
                memberId,
                "google-" + memberId,
                "member" + memberId + "@example.com",
                "Member",
                WorkspaceMemberRole.MEMBER,
                LocalDateTime.of(2026, 5, 15, 11, 0));
    }

    private static List<ListWorkspaceMembersResult> members(ListWorkspaceMembersResult... members) {
        return List.of(members);
    }

    private static class RecordingPresenceEventSink implements WorkspacePresenceEventSink {
        private final List<WorkspacePresenceMessage> messages = new ArrayList<>();

        @Override
        public void send(WorkspacePresenceMessage message) {
            messages.add(message);
        }

        private List<WorkspacePresenceMessage> messages() {
            return messages;
        }

        private void clear() {
            messages.clear();
        }
    }

    private static class FakeLoadMemberPort implements LoadMemberPort {

        @Override
        public Optional<Member> findByGoogleSub(String googleSub) {
            return Optional.of(Member.createUser(googleSub, googleSub + "@example.com", "Requester"));
        }
    }

    private static class FakeLoadWorkspacePort implements LoadWorkspacePort {
        private final Optional<GetWorkspaceDetailResult> detailResult;

        private FakeLoadWorkspacePort(Optional<GetWorkspaceDetailResult> detailResult) {
            this.detailResult = detailResult;
        }

        @Override
        public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            throw new UnsupportedOperationException("not used in workspace presence stream service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId, String googleSub) {
            return detailResult;
        }
    }

    private static class FakeLoadWorkspaceMemberPort implements LoadWorkspaceMemberPort {
        private final List<ListWorkspaceMembersResult> members;

        private FakeLoadWorkspaceMemberPort(List<ListWorkspaceMembersResult> members) {
            this.members = members;
        }

        @Override
        public List<ListWorkspaceMembersResult> findActiveWorkspaceMembersByWorkspaceId(Long workspaceId) {
            return members;
        }
    }
}
