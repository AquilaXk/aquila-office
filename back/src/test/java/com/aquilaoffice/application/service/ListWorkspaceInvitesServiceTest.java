package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceInvitesResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceInvitesPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.WorkspaceInviteManagement;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 초대 목록 조회 service가 관리자 멤버십을 확인한 뒤 저장소 목록 조회를 호출하는지 검증한다.
 */
class ListWorkspaceInvitesServiceTest {

    @Test
    void listWorkspaceInvitesReturnsInvitesWhenRequesterIsAdmin() {
        Member requester = Member.createUser("google-1", "admin@example.com", "Admin");
        FakeLoadMemberPort loadMemberPort = new FakeLoadMemberPort(Optional.of(requester));
        FakeLoadWorkspacePort loadWorkspacePort = new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN)));
        FakeLoadWorkspaceInvitesPort loadWorkspaceInvitesPort = new FakeLoadWorkspaceInvitesPort(List.of(
                new WorkspaceInviteManagement(
                        501L,
                        101L,
                        "invitee@example.com",
                        "invite-token",
                        WorkspaceInviteStatus.PENDING,
                        LocalDateTime.of(2026, 5, 15, 10, 0),
                        LocalDateTime.of(2026, 5, 22, 10, 0),
                        null,
                        null)));
        ListWorkspaceInvitesService service =
                new ListWorkspaceInvitesService(loadMemberPort, loadWorkspacePort, loadWorkspaceInvitesPort);

        List<ListWorkspaceInvitesResult> results = service.listWorkspaceInvites(
                new ListWorkspaceInvitesQuery(101L, " google-1 ", WorkspaceInviteStatus.PENDING));

        assertThat(loadMemberPort.requestedGoogleSub).isEqualTo("google-1");
        assertThat(loadWorkspacePort.requestedWorkspaceId).isEqualTo(101L);
        assertThat(loadWorkspacePort.requestedGoogleSub).isEqualTo("google-1");
        assertThat(loadWorkspaceInvitesPort.requestedWorkspaceId).isEqualTo(101L);
        assertThat(loadWorkspaceInvitesPort.requestedStatus).isEqualTo(WorkspaceInviteStatus.PENDING);
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().inviteId()).isEqualTo(501L);
        assertThat(results.getFirst().token()).isEqualTo("invite-token");
    }

    @Test
    void listWorkspaceInvitesRejectsNonAdminMembership() {
        Member requester = Member.createUser("google-1", "member@example.com", "Member");
        FakeLoadWorkspaceInvitesPort loadWorkspaceInvitesPort = new FakeLoadWorkspaceInvitesPort(List.of());
        ListWorkspaceInvitesService service = new ListWorkspaceInvitesService(
                new FakeLoadMemberPort(Optional.of(requester)),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                loadWorkspaceInvitesPort);

        assertThatThrownBy(() -> service.listWorkspaceInvites(
                        new ListWorkspaceInvitesQuery(101L, "google-1", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");
        assertThat(loadWorkspaceInvitesPort.called).isFalse();
    }

    @Test
    void listWorkspaceInvitesRejectsUnknownRequesterAndDoesNotQueryWorkspace() {
        FakeLoadWorkspacePort loadWorkspacePort = new FakeLoadWorkspacePort(Optional.empty());
        FakeLoadWorkspaceInvitesPort loadWorkspaceInvitesPort = new FakeLoadWorkspaceInvitesPort(List.of());
        ListWorkspaceInvitesService service = new ListWorkspaceInvitesService(
                new FakeLoadMemberPort(Optional.empty()), loadWorkspacePort, loadWorkspaceInvitesPort);

        assertThatThrownBy(() -> service.listWorkspaceInvites(
                        new ListWorkspaceInvitesQuery(101L, "missing-google", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member not found");
        assertThat(loadWorkspacePort.called).isFalse();
        assertThat(loadWorkspaceInvitesPort.called).isFalse();
    }

    @Test
    void queryRejectsInvalidRequiredValues() {
        assertThatThrownBy(() -> new ListWorkspaceInvitesQuery(0L, "google-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must be positive");
        assertThatThrownBy(() -> new ListWorkspaceInvitesQuery(101L, " ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("memberGoogleSub must not be blank");
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

    private static class FakeLoadMemberPort implements LoadMemberPort {
        private final Optional<Member> member;
        private String requestedGoogleSub;

        private FakeLoadMemberPort(Optional<Member> member) {
            this.member = member;
        }

        @Override
        public Optional<Member> findByGoogleSub(String googleSub) {
            this.requestedGoogleSub = googleSub;
            return member;
        }
    }

    private static class FakeLoadWorkspacePort implements LoadWorkspacePort {
        private final Optional<GetWorkspaceDetailResult> detailResult;
        private Long requestedWorkspaceId;
        private String requestedGoogleSub;
        private boolean called;

        private FakeLoadWorkspacePort(Optional<GetWorkspaceDetailResult> detailResult) {
            this.detailResult = detailResult;
        }

        @Override
        public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            throw new UnsupportedOperationException("not used in invite list service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId, String googleSub) {
            this.called = true;
            this.requestedWorkspaceId = workspaceId;
            this.requestedGoogleSub = googleSub;
            return detailResult;
        }
    }

    private static class FakeLoadWorkspaceInvitesPort implements LoadWorkspaceInvitesPort {
        private final List<WorkspaceInviteManagement> invites;
        private Long requestedWorkspaceId;
        private WorkspaceInviteStatus requestedStatus;
        private boolean called;

        private FakeLoadWorkspaceInvitesPort(List<WorkspaceInviteManagement> invites) {
            this.invites = invites;
        }

        @Override
        public List<WorkspaceInviteManagement> findWorkspaceInvites(
                Long workspaceId, WorkspaceInviteStatus status) {
            this.called = true;
            this.requestedWorkspaceId = workspaceId;
            this.requestedStatus = status;
            return invites;
        }
    }
}
