package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.UpdateWorkspacePresencePositionCommand;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import com.aquilaoffice.domain.workspace.WorkspacePresencePosition;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 presence 위치 갱신 service가 요청자 멤버십을 검증하고 최신 위치를 기록하는지 확인한다.
 */
class UpdateWorkspacePresencePositionServiceTest {

    @Test
    void updatePositionRecordsRequesterPresenceState() {
        WorkspacePresenceRegistry registry = new WorkspacePresenceRegistry();
        UpdateWorkspacePresencePositionService service = service(
                "google-2",
                Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER)),
                members(adminMember(1L), memberMember(2L)),
                registry);

        service.updatePosition(new UpdateWorkspacePresencePositionCommand(
                101L,
                " google-2 ",
                new WorkspacePresencePosition(" 18px ", " 24px "),
                " reviewing "));

        Optional<WorkspacePresenceRegistry.MemberState> state = registry.findMemberState(101L, 2L);
        assertThat(state).isPresent();
        assertThat(state.get().position().left()).isEqualTo("18px");
        assertThat(state.get().position().top()).isEqualTo("24px");
        assertThat(state.get().status()).isEqualTo("reviewing");
    }

    @Test
    void updatePositionRejectsMissingMember() {
        WorkspacePresenceRegistry registry = new WorkspacePresenceRegistry();
        UpdateWorkspacePresencePositionService service = new UpdateWorkspacePresencePositionService(
                new FakeLoadMemberPort(Optional.empty()),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                new FakeLoadWorkspaceMemberPort(members(memberMember(2L))),
                registry);

        assertThatThrownBy(() -> service.updatePosition(new UpdateWorkspacePresencePositionCommand(
                        101L,
                        "missing-google",
                        new WorkspacePresencePosition("10px", "20px"),
                        null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member not found");
        assertThat(registry.findMemberState(101L, 2L)).isEmpty();
    }

    @Test
    void updatePositionRejectsNotJoinedWorkspace() {
        WorkspacePresenceRegistry registry = new WorkspacePresenceRegistry();
        UpdateWorkspacePresencePositionService service = service(
                "google-2",
                Optional.empty(),
                members(memberMember(2L)),
                registry);

        assertThatThrownBy(() -> service.updatePosition(new UpdateWorkspacePresencePositionCommand(
                        101L,
                        "google-2",
                        new WorkspacePresencePosition("10px", "20px"),
                        "online")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace not found");
        assertThat(registry.findMemberState(101L, 2L)).isEmpty();
    }

    @Test
    void commandRejectsInvalidRequiredValues() {
        assertThatThrownBy(() -> new UpdateWorkspacePresencePositionCommand(
                        0L,
                        "google-2",
                        new WorkspacePresencePosition("10px", "20px"),
                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must be positive");
        assertThatThrownBy(() -> new UpdateWorkspacePresencePositionCommand(
                        101L,
                        " ",
                        new WorkspacePresencePosition("10px", "20px"),
                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("memberGoogleSub must not be blank");
        assertThatThrownBy(() -> new WorkspacePresencePosition(" ", "20px"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("left must not be blank");
        assertThatThrownBy(() -> new WorkspacePresencePosition("10px", "x".repeat(31)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("top must be 30 characters or less");
        assertThatThrownBy(() -> new UpdateWorkspacePresencePositionCommand(
                        101L,
                        "google-2",
                        new WorkspacePresencePosition("10px", "20px"),
                        "x".repeat(31)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("status must be 30 characters or less");
    }

    private UpdateWorkspacePresencePositionService service(
            String requesterGoogleSub,
            Optional<GetWorkspaceDetailResult> workspaceDetail,
            List<ListWorkspaceMembersResult> members,
            WorkspacePresenceRegistry registry) {
        return new UpdateWorkspacePresencePositionService(
                new FakeLoadMemberPort(Optional.of(Member.createUser(
                        requesterGoogleSub, requesterGoogleSub + "@example.com", "Requester"))),
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
            throw new UnsupportedOperationException("not used in update workspace presence position service test");
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
