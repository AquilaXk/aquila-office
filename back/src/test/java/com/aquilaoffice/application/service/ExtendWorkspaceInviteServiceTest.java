package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.ExtendWorkspaceInviteCommand;
import com.aquilaoffice.application.port.in.ExtendWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.out.ExtendWorkspaceInvitePort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.WorkspaceInviteExtension;
import com.aquilaoffice.application.port.out.WorkspaceInviteManagement;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 초대 연장 service가 관리자 멤버십과 연장일 정책을 검증하는지 확인한다.
 */
class ExtendWorkspaceInviteServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-05-15T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void extendWorkspaceInviteCallsPortWhenRequesterIsAdmin() {
        FakeExtendWorkspaceInvitePort extendWorkspaceInvitePort =
                new FakeExtendWorkspaceInvitePort(management(LocalDateTime.of(2026, 5, 25, 12, 0)));
        ExtendWorkspaceInviteService service = new ExtendWorkspaceInviteService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                extendWorkspaceInvitePort,
                CLOCK);

        ExtendWorkspaceInviteResult result = service.extendWorkspaceInvite(
                new ExtendWorkspaceInviteCommand(101L, 501L, " google-1 ", 3));

        assertThat(extendWorkspaceInvitePort.extension.workspaceId()).isEqualTo(101L);
        assertThat(extendWorkspaceInvitePort.extension.inviteId()).isEqualTo(501L);
        assertThat(extendWorkspaceInvitePort.extension.additionalDays()).isEqualTo(3);
        assertThat(extendWorkspaceInvitePort.extension.extendedAt())
                .isEqualTo(LocalDateTime.of(2026, 5, 15, 12, 0));
        assertThat(result.expiresAt()).isEqualTo(LocalDateTime.of(2026, 5, 25, 12, 0));
    }

    @Test
    void extendWorkspaceInviteUsesDefaultDaysWhenAdditionalDaysIsNull() {
        FakeExtendWorkspaceInvitePort extendWorkspaceInvitePort =
                new FakeExtendWorkspaceInvitePort(management(LocalDateTime.of(2026, 5, 29, 12, 0)));
        ExtendWorkspaceInviteService service = new ExtendWorkspaceInviteService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                extendWorkspaceInvitePort,
                CLOCK);

        service.extendWorkspaceInvite(new ExtendWorkspaceInviteCommand(101L, 501L, "google-1", null));

        assertThat(extendWorkspaceInvitePort.extension.additionalDays()).isEqualTo(7);
    }

    @Test
    void extendWorkspaceInviteRejectsNonAdminMembership() {
        FakeExtendWorkspaceInvitePort extendWorkspaceInvitePort =
                new FakeExtendWorkspaceInvitePort(management(LocalDateTime.of(2026, 5, 25, 12, 0)));
        ExtendWorkspaceInviteService service = new ExtendWorkspaceInviteService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                extendWorkspaceInvitePort,
                CLOCK);

        assertThatThrownBy(() -> service.extendWorkspaceInvite(
                        new ExtendWorkspaceInviteCommand(101L, 501L, "google-1", 3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");
        assertThat(extendWorkspaceInvitePort.called).isFalse();
    }

    @Test
    void extendWorkspaceInviteRejectsUnknownRequesterAndDoesNotQueryWorkspace() {
        FakeLoadWorkspacePort loadWorkspacePort = new FakeLoadWorkspacePort(Optional.empty());
        FakeExtendWorkspaceInvitePort extendWorkspaceInvitePort =
                new FakeExtendWorkspaceInvitePort(management(LocalDateTime.of(2026, 5, 25, 12, 0)));
        ExtendWorkspaceInviteService service = new ExtendWorkspaceInviteService(
                new FakeLoadMemberPort(Optional.empty()), loadWorkspacePort, extendWorkspaceInvitePort, CLOCK);

        assertThatThrownBy(() -> service.extendWorkspaceInvite(
                        new ExtendWorkspaceInviteCommand(101L, 501L, "missing-google", 3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member not found");
        assertThat(loadWorkspacePort.called).isFalse();
        assertThat(extendWorkspaceInvitePort.called).isFalse();
    }

    @Test
    void commandRejectsInvalidValues() {
        assertThatThrownBy(() -> new ExtendWorkspaceInviteCommand(0L, 501L, "google-1", 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must be positive");
        assertThatThrownBy(() -> new ExtendWorkspaceInviteCommand(101L, 0L, "google-1", 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("inviteId must be positive");
        assertThatThrownBy(() -> new ExtendWorkspaceInviteCommand(101L, 501L, " ", 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requesterGoogleSub must not be blank");
        assertThatThrownBy(() -> new ExtendWorkspaceInviteCommand(101L, 501L, "google-1", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("additionalDays must be between 1 and 30");
        assertThatThrownBy(() -> new ExtendWorkspaceInviteCommand(101L, 501L, "google-1", 31))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("additionalDays must be between 1 and 30");
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

    private static WorkspaceInviteManagement management(LocalDateTime expiresAt) {
        return new WorkspaceInviteManagement(
                501L,
                101L,
                "invitee@example.com",
                "invite-token",
                WorkspaceInviteStatus.PENDING,
                LocalDateTime.of(2026, 5, 15, 12, 0),
                expiresAt,
                null,
                null);
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
            throw new UnsupportedOperationException("not used in invite extension service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId, String googleSub) {
            this.called = true;
            return detailResult;
        }
    }

    private static class FakeExtendWorkspaceInvitePort implements ExtendWorkspaceInvitePort {
        private final WorkspaceInviteManagement result;
        private WorkspaceInviteExtension extension;
        private boolean called;

        private FakeExtendWorkspaceInvitePort(WorkspaceInviteManagement result) {
            this.result = result;
        }

        @Override
        public WorkspaceInviteManagement extend(WorkspaceInviteExtension extension) {
            this.called = true;
            this.extension = extension;
            return result;
        }
    }
}
