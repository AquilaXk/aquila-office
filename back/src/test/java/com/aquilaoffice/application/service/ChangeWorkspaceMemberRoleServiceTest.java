package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.ChangeWorkspaceMemberRoleCommand;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.out.ChangeWorkspaceMemberRolePort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.WorkspaceMemberRoleChange;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 멤버 역할 변경 service가 관리자 권한과 마지막 ADMIN 보호 규칙을 검증하는지 확인한다.
 */
class ChangeWorkspaceMemberRoleServiceTest {

    @Test
    void changeWorkspaceMemberRoleCallsPortWhenRequesterIsAdmin() {
        FakeChangeWorkspaceMemberRolePort changePort = new FakeChangeWorkspaceMemberRolePort();
        ChangeWorkspaceMemberRoleService service = service(
                WorkspaceMemberRole.ADMIN,
                members(adminMember(1L), memberMember(2L)),
                changePort);

        service.changeWorkspaceMemberRole(new ChangeWorkspaceMemberRoleCommand(
                101L,
                2L,
                " google-1 ",
                WorkspaceMemberRole.ADMIN));

        assertThat(changePort.change.workspaceId()).isEqualTo(101L);
        assertThat(changePort.change.memberId()).isEqualTo(2L);
        assertThat(changePort.change.role()).isEqualTo(WorkspaceMemberRole.ADMIN);
    }

    @Test
    void changeWorkspaceMemberRoleRejectsNonAdminRequester() {
        FakeChangeWorkspaceMemberRolePort changePort = new FakeChangeWorkspaceMemberRolePort();
        ChangeWorkspaceMemberRoleService service = service(
                WorkspaceMemberRole.MEMBER,
                members(adminMember(1L), memberMember(2L)),
                changePort);

        assertThatThrownBy(() -> service.changeWorkspaceMemberRole(new ChangeWorkspaceMemberRoleCommand(
                        101L,
                        2L,
                        "google-1",
                        WorkspaceMemberRole.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");
        assertThat(changePort.called).isFalse();
    }

    @Test
    void changeWorkspaceMemberRoleRejectsMissingTargetMember() {
        FakeChangeWorkspaceMemberRolePort changePort = new FakeChangeWorkspaceMemberRolePort();
        ChangeWorkspaceMemberRoleService service = service(
                WorkspaceMemberRole.ADMIN,
                members(adminMember(1L)),
                changePort);

        assertThatThrownBy(() -> service.changeWorkspaceMemberRole(new ChangeWorkspaceMemberRoleCommand(
                        101L,
                        404L,
                        "google-1",
                        WorkspaceMemberRole.MEMBER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace member not found");
        assertThat(changePort.called).isFalse();
    }

    @Test
    void changeWorkspaceMemberRoleRejectsChangingLastAdminToMember() {
        FakeChangeWorkspaceMemberRolePort changePort = new FakeChangeWorkspaceMemberRolePort();
        ChangeWorkspaceMemberRoleService service = service(
                WorkspaceMemberRole.ADMIN,
                members(adminMember(1L), memberMember(2L)),
                changePort);

        assertThatThrownBy(() -> service.changeWorkspaceMemberRole(new ChangeWorkspaceMemberRoleCommand(
                        101L,
                        1L,
                        "google-1",
                        WorkspaceMemberRole.MEMBER)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("last admin cannot be changed");
        assertThat(changePort.called).isFalse();
    }

    @Test
    void commandRejectsInvalidRequiredValues() {
        assertThatThrownBy(() -> new ChangeWorkspaceMemberRoleCommand(0L, 2L, "google-1", WorkspaceMemberRole.MEMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must be positive");
        assertThatThrownBy(() -> new ChangeWorkspaceMemberRoleCommand(101L, 0L, "google-1", WorkspaceMemberRole.MEMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("memberId must be positive");
        assertThatThrownBy(() -> new ChangeWorkspaceMemberRoleCommand(101L, 2L, " ", WorkspaceMemberRole.MEMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requesterGoogleSub must not be blank");
        assertThatThrownBy(() -> new ChangeWorkspaceMemberRoleCommand(101L, 2L, "google-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("role must not be null");
    }

    private ChangeWorkspaceMemberRoleService service(
            WorkspaceMemberRole requesterRole,
            List<ListWorkspaceMembersResult> members,
            FakeChangeWorkspaceMemberRolePort changePort) {
        return new ChangeWorkspaceMemberRoleService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(requesterRole))),
                new FakeLoadWorkspaceMemberPort(members),
                changePort);
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

        private FakeLoadWorkspacePort(Optional<GetWorkspaceDetailResult> detailResult) {
            this.detailResult = detailResult;
        }

        @Override
        public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            throw new UnsupportedOperationException("not used in member role service test");
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

    private static class FakeChangeWorkspaceMemberRolePort implements ChangeWorkspaceMemberRolePort {
        private WorkspaceMemberRoleChange change;
        private boolean called;

        @Override
        public void changeRole(WorkspaceMemberRoleChange change) {
            this.called = true;
            this.change = change;
        }
    }
}
