package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.RemoveWorkspaceMemberCommand;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.RemoveWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.WorkspaceMemberRemoval;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 멤버 제거 service가 관리자 권한과 마지막 ADMIN 보호 규칙을 검증하는지 확인한다.
 */
class RemoveWorkspaceMemberServiceTest {

    @Test
    void removeWorkspaceMemberCallsPortWhenRequesterIsAdmin() {
        FakeRemoveWorkspaceMemberPort removePort = new FakeRemoveWorkspaceMemberPort();
        RemoveWorkspaceMemberService service = service(
                WorkspaceMemberRole.ADMIN,
                members(adminMember(1L), memberMember(2L)),
                removePort);

        service.removeWorkspaceMember(new RemoveWorkspaceMemberCommand(101L, 2L, " google-1 "));

        assertThat(removePort.removal.workspaceId()).isEqualTo(101L);
        assertThat(removePort.removal.memberId()).isEqualTo(2L);
    }

    @Test
    void removeWorkspaceMemberRejectsNonAdminRequester() {
        FakeRemoveWorkspaceMemberPort removePort = new FakeRemoveWorkspaceMemberPort();
        RemoveWorkspaceMemberService service = service(
                WorkspaceMemberRole.MEMBER,
                members(adminMember(1L), memberMember(2L)),
                removePort);

        assertThatThrownBy(() -> service.removeWorkspaceMember(
                        new RemoveWorkspaceMemberCommand(101L, 2L, "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");
        assertThat(removePort.called).isFalse();
    }

    @Test
    void removeWorkspaceMemberRejectsMissingTargetMember() {
        FakeRemoveWorkspaceMemberPort removePort = new FakeRemoveWorkspaceMemberPort();
        RemoveWorkspaceMemberService service = service(
                WorkspaceMemberRole.ADMIN,
                members(adminMember(1L)),
                removePort);

        assertThatThrownBy(() -> service.removeWorkspaceMember(
                        new RemoveWorkspaceMemberCommand(101L, 404L, "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace member not found");
        assertThat(removePort.called).isFalse();
    }

    @Test
    void removeWorkspaceMemberRejectsRemovingLastAdmin() {
        FakeRemoveWorkspaceMemberPort removePort = new FakeRemoveWorkspaceMemberPort();
        RemoveWorkspaceMemberService service = service(
                WorkspaceMemberRole.ADMIN,
                members(adminMember(1L), memberMember(2L)),
                removePort);

        assertThatThrownBy(() -> service.removeWorkspaceMember(
                        new RemoveWorkspaceMemberCommand(101L, 1L, "google-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("last admin cannot be removed");
        assertThat(removePort.called).isFalse();
    }

    @Test
    void commandRejectsInvalidRequiredValues() {
        assertThatThrownBy(() -> new RemoveWorkspaceMemberCommand(0L, 2L, "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must be positive");
        assertThatThrownBy(() -> new RemoveWorkspaceMemberCommand(101L, 0L, "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("memberId must be positive");
        assertThatThrownBy(() -> new RemoveWorkspaceMemberCommand(101L, 2L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requesterGoogleSub must not be blank");
    }

    private RemoveWorkspaceMemberService service(
            WorkspaceMemberRole requesterRole,
            List<ListWorkspaceMembersResult> members,
            FakeRemoveWorkspaceMemberPort removePort) {
        return new RemoveWorkspaceMemberService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(requesterRole))),
                new FakeLoadWorkspaceMemberPort(members),
                removePort);
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
            throw new UnsupportedOperationException("not used in remove member service test");
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

    private static class FakeRemoveWorkspaceMemberPort implements RemoveWorkspaceMemberPort {
        private WorkspaceMemberRemoval removal;
        private boolean called;

        @Override
        public void remove(WorkspaceMemberRemoval removal) {
            this.called = true;
            this.removal = removal;
        }
    }
}
