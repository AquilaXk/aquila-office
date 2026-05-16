package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.LeaveWorkspaceCommand;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
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
 * 워크스페이스 나가기 service가 요청자 본인 멤버십만 제거하고 마지막 ADMIN 규칙을 지키는지 확인한다.
 */
class LeaveWorkspaceServiceTest {

    @Test
    void leaveWorkspaceRemovesRequesterMembership() {
        FakeRemoveWorkspaceMemberPort removePort = new FakeRemoveWorkspaceMemberPort();
        LeaveWorkspaceService service = service(
                "google-2",
                Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER)),
                members(adminMember(1L), memberMember(2L)),
                removePort);

        service.leaveWorkspace(new LeaveWorkspaceCommand(101L, " google-2 "));

        assertThat(removePort.removal.workspaceId()).isEqualTo(101L);
        assertThat(removePort.removal.memberId()).isEqualTo(2L);
    }

    @Test
    void leaveWorkspaceRejectsMissingRequesterMembership() {
        FakeRemoveWorkspaceMemberPort removePort = new FakeRemoveWorkspaceMemberPort();
        LeaveWorkspaceService service = service(
                "google-2",
                Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER)),
                members(adminMember(1L)),
                removePort);

        assertThatThrownBy(() -> service.leaveWorkspace(new LeaveWorkspaceCommand(101L, "google-2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace member not found");
        assertThat(removePort.called).isFalse();
    }

    @Test
    void leaveWorkspaceRejectsLastAdminLeave() {
        FakeRemoveWorkspaceMemberPort removePort = new FakeRemoveWorkspaceMemberPort();
        LeaveWorkspaceService service = service(
                "google-1",
                Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN)),
                members(adminMember(1L), memberMember(2L)),
                removePort);

        assertThatThrownBy(() -> service.leaveWorkspace(new LeaveWorkspaceCommand(101L, "google-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("last admin cannot leave workspace");
        assertThat(removePort.called).isFalse();
    }

    @Test
    void commandRejectsInvalidRequiredValues() {
        assertThatThrownBy(() -> new LeaveWorkspaceCommand(0L, "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must be positive");
        assertThatThrownBy(() -> new LeaveWorkspaceCommand(101L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requesterGoogleSub must not be blank");
    }

    private LeaveWorkspaceService service(
            String requesterGoogleSub,
            Optional<GetWorkspaceDetailResult> workspaceDetail,
            List<ListWorkspaceMembersResult> members,
            FakeRemoveWorkspaceMemberPort removePort) {
        return new LeaveWorkspaceService(
                new FakeLoadMemberPort(Optional.of(Member.createUser(
                        requesterGoogleSub, requesterGoogleSub + "@example.com", "Requester"))),
                new FakeLoadWorkspacePort(workspaceDetail),
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
            throw new UnsupportedOperationException("not used in leave workspace service test");
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
