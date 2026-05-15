package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceMembersResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 멤버 목록 조회 service가 요청 회원과 워크스페이스 가입 여부를 검증하는지 확인한다.
 */
class ListWorkspaceMembersServiceTest {

    @Test
    void listWorkspaceMembersReturnsMembersWhenRequesterJoinedWorkspace() {
        Member requester = Member.createUser("google-1", "admin@example.com", "Admin");
        FakeLoadMemberPort loadMemberPort = new FakeLoadMemberPort(Optional.of(requester));
        FakeLoadWorkspacePort loadWorkspacePort = new FakeLoadWorkspacePort(Optional.of(new GetWorkspaceDetailResult(
                101L,
                "Team",
                "Description",
                "google-1",
                WorkspaceMemberRole.ADMIN,
                LocalDateTime.of(2026, 5, 15, 10, 0))));
        FakeLoadWorkspaceMemberPort loadWorkspaceMemberPort = new FakeLoadWorkspaceMemberPort(List.of(
                new ListWorkspaceMembersResult(
                        1L,
                        "google-1",
                        "admin@example.com",
                        "Admin",
                        WorkspaceMemberRole.ADMIN,
                        LocalDateTime.of(2026, 5, 15, 10, 0)),
                new ListWorkspaceMembersResult(
                        2L,
                        "google-2",
                        "member@example.com",
                        "Member",
                        WorkspaceMemberRole.MEMBER,
                        LocalDateTime.of(2026, 5, 15, 11, 0))));
        ListWorkspaceMembersService service =
                new ListWorkspaceMembersService(loadMemberPort, loadWorkspacePort, loadWorkspaceMemberPort);

        List<ListWorkspaceMembersResult> results =
                service.listWorkspaceMembers(new ListWorkspaceMembersQuery(101L, " google-1 "));

        assertThat(loadMemberPort.requestedGoogleSub).isEqualTo("google-1");
        assertThat(loadWorkspacePort.requestedWorkspaceId).isEqualTo(101L);
        assertThat(loadWorkspacePort.requestedGoogleSub).isEqualTo("google-1");
        assertThat(loadWorkspaceMemberPort.requestedWorkspaceId).isEqualTo(101L);
        assertThat(results)
                .extracting(ListWorkspaceMembersResult::memberGoogleSub)
                .containsExactly("google-1", "google-2");
    }

    @Test
    void listWorkspaceMembersRejectsUnknownMemberAndDoesNotQueryWorkspace() {
        FakeLoadMemberPort loadMemberPort = new FakeLoadMemberPort(Optional.empty());
        FakeLoadWorkspacePort loadWorkspacePort = new FakeLoadWorkspacePort(Optional.empty());
        FakeLoadWorkspaceMemberPort loadWorkspaceMemberPort = new FakeLoadWorkspaceMemberPort(List.of());
        ListWorkspaceMembersService service =
                new ListWorkspaceMembersService(loadMemberPort, loadWorkspacePort, loadWorkspaceMemberPort);

        assertThatThrownBy(() -> service.listWorkspaceMembers(new ListWorkspaceMembersQuery(101L, "missing")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member not found");
        assertThat(loadWorkspacePort.called).isFalse();
        assertThat(loadWorkspaceMemberPort.called).isFalse();
    }

    @Test
    void listWorkspaceMembersRejectsUnjoinedWorkspace() {
        Member requester = Member.createUser("google-1", "admin@example.com", "Admin");
        FakeLoadWorkspaceMemberPort loadWorkspaceMemberPort = new FakeLoadWorkspaceMemberPort(List.of());
        ListWorkspaceMembersService service = new ListWorkspaceMembersService(
                new FakeLoadMemberPort(Optional.of(requester)),
                new FakeLoadWorkspacePort(Optional.empty()),
                loadWorkspaceMemberPort);

        assertThatThrownBy(() -> service.listWorkspaceMembers(new ListWorkspaceMembersQuery(101L, "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace not found");
        assertThat(loadWorkspaceMemberPort.called).isFalse();
    }

    @Test
    void queryRejectsInvalidWorkspaceId() {
        assertThatThrownBy(() -> new ListWorkspaceMembersQuery(0L, "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must be positive");
    }

    @Test
    void queryRejectsBlankGoogleSub() {
        assertThatThrownBy(() -> new ListWorkspaceMembersQuery(101L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("memberGoogleSub must not be blank");
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
        public List<com.aquilaoffice.application.port.in.ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(
                String googleSub) {
            throw new UnsupportedOperationException("not used in member list service test");
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

    private static class FakeLoadWorkspaceMemberPort implements LoadWorkspaceMemberPort {
        private final List<ListWorkspaceMembersResult> results;
        private Long requestedWorkspaceId;
        private boolean called;

        private FakeLoadWorkspaceMemberPort(List<ListWorkspaceMembersResult> results) {
            this.results = results;
        }

        @Override
        public List<ListWorkspaceMembersResult> findActiveWorkspaceMembersByWorkspaceId(Long workspaceId) {
            this.called = true;
            this.requestedWorkspaceId = workspaceId;
            return results;
        }
    }
}
