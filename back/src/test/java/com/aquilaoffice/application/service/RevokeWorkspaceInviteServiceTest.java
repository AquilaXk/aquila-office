package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.RevokeWorkspaceInviteCommand;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.RevokeWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.WorkspaceInviteRevocation;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 초대 취소 service가 관리자 멤버십을 확인한 뒤 취소 저장 port를 호출하는지 검증한다.
 */
class RevokeWorkspaceInviteServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-05-15T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void revokeWorkspaceInviteCallsPortWhenRequesterIsAdmin() {
        Member requester = Member.createUser("google-1", "admin@example.com", "Admin");
        FakeRevokeWorkspaceInvitePort revokeWorkspaceInvitePort = new FakeRevokeWorkspaceInvitePort();
        RevokeWorkspaceInviteService service = new RevokeWorkspaceInviteService(
                new FakeLoadMemberPort(Optional.of(requester)),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                revokeWorkspaceInvitePort,
                CLOCK);

        service.revokeWorkspaceInvite(new RevokeWorkspaceInviteCommand(101L, 501L, " google-1 "));

        assertThat(revokeWorkspaceInvitePort.revocation.workspaceId()).isEqualTo(101L);
        assertThat(revokeWorkspaceInvitePort.revocation.inviteId()).isEqualTo(501L);
        assertThat(revokeWorkspaceInvitePort.revocation.revokedAt())
                .isEqualTo(LocalDateTime.of(2026, 5, 15, 12, 0));
    }

    @Test
    void revokeWorkspaceInviteRejectsNonAdminMembership() {
        FakeRevokeWorkspaceInvitePort revokeWorkspaceInvitePort = new FakeRevokeWorkspaceInvitePort();
        RevokeWorkspaceInviteService service = new RevokeWorkspaceInviteService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                revokeWorkspaceInvitePort,
                CLOCK);

        assertThatThrownBy(() -> service.revokeWorkspaceInvite(
                        new RevokeWorkspaceInviteCommand(101L, 501L, "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");
        assertThat(revokeWorkspaceInvitePort.called).isFalse();
    }

    @Test
    void revokeWorkspaceInviteRejectsUnknownRequesterAndDoesNotQueryWorkspace() {
        FakeLoadWorkspacePort loadWorkspacePort = new FakeLoadWorkspacePort(Optional.empty());
        FakeRevokeWorkspaceInvitePort revokeWorkspaceInvitePort = new FakeRevokeWorkspaceInvitePort();
        RevokeWorkspaceInviteService service = new RevokeWorkspaceInviteService(
                new FakeLoadMemberPort(Optional.empty()), loadWorkspacePort, revokeWorkspaceInvitePort, CLOCK);

        assertThatThrownBy(() -> service.revokeWorkspaceInvite(
                        new RevokeWorkspaceInviteCommand(101L, 501L, "missing-google")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member not found");
        assertThat(loadWorkspacePort.called).isFalse();
        assertThat(revokeWorkspaceInvitePort.called).isFalse();
    }

    @Test
    void commandRejectsInvalidRequiredValues() {
        assertThatThrownBy(() -> new RevokeWorkspaceInviteCommand(0L, 501L, "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must be positive");
        assertThatThrownBy(() -> new RevokeWorkspaceInviteCommand(101L, 0L, "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("inviteId must be positive");
        assertThatThrownBy(() -> new RevokeWorkspaceInviteCommand(101L, 501L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requesterGoogleSub must not be blank");
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

        private FakeLoadMemberPort(Optional<Member> member) {
            this.member = member;
        }

        @Override
        public Optional<Member> findByGoogleSub(String googleSub) {
            return member;
        }
    }

    private static class FakeLoadWorkspacePort implements LoadWorkspacePort {
        private final Optional<GetWorkspaceDetailResult> detailResult;
        private boolean called;

        private FakeLoadWorkspacePort(Optional<GetWorkspaceDetailResult> detailResult) {
            this.detailResult = detailResult;
        }

        @Override
        public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            throw new UnsupportedOperationException("not used in invite revoke service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId, String googleSub) {
            this.called = true;
            return detailResult;
        }
    }

    private static class FakeRevokeWorkspaceInvitePort implements RevokeWorkspaceInvitePort {
        private WorkspaceInviteRevocation revocation;
        private boolean called;

        @Override
        public void revoke(WorkspaceInviteRevocation revocation) {
            this.called = true;
            this.revocation = revocation;
        }
    }
}
