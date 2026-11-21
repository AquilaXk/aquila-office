package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.ListWorkspacesQuery;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 목록 조회 application service가 회원 검증 후 조회 port를 호출하는지 검증한다.
 */
class ListWorkspacesServiceTest {

    @Test
    void listWorkspacesReturnsJoinedWorkspaceResults() {
        Member member = Member.createUser("google-1", "user@example.com", "User");
        FakeLoadMemberPort loadMemberPort = new FakeLoadMemberPort(Optional.of(member));
        FakeLoadWorkspacePort loadWorkspacePort = new FakeLoadWorkspacePort(List.of(new ListWorkspacesResult(
                101L,
                "Team",
                "Description",
                "google-1",
                WorkspaceMemberRole.ADMIN,
                LocalDateTime.of(2026, 5, 15, 10, 0))));
        ListWorkspacesService service = new ListWorkspacesService(loadMemberPort, loadWorkspacePort);

        List<ListWorkspacesResult> results = service.listWorkspaces(new ListWorkspacesQuery(" google-1 "));

        assertThat(loadMemberPort.requestedGoogleSub).isEqualTo("google-1");
        assertThat(loadWorkspacePort.requestedGoogleSub).isEqualTo("google-1");
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().workspaceId()).isEqualTo(101L);
        assertThat(results.getFirst().name()).isEqualTo("Team");
        assertThat(results.getFirst().description()).isEqualTo("Description");
        assertThat(results.getFirst().creatorGoogleSub()).isEqualTo("google-1");
        assertThat(results.getFirst().myRole()).isEqualTo(WorkspaceMemberRole.ADMIN);
        assertThat(results.getFirst().joinedAt()).isEqualTo(LocalDateTime.of(2026, 5, 15, 10, 0));
    }

    @Test
    void listWorkspacesRejectsUnknownMemberAndDoesNotQueryWorkspaces() {
        FakeLoadMemberPort loadMemberPort = new FakeLoadMemberPort(Optional.empty());
        FakeLoadWorkspacePort loadWorkspacePort = new FakeLoadWorkspacePort(List.of());
        ListWorkspacesService service = new ListWorkspacesService(loadMemberPort, loadWorkspacePort);

        assertThatThrownBy(() -> service.listWorkspaces(new ListWorkspacesQuery("missing")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member not found");
        assertThat(loadWorkspacePort.called).isFalse();
    }

    @Test
    void queryRejectsBlankGoogleSub() {
        assertThatThrownBy(() -> new ListWorkspacesQuery(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("memberGoogleSub must not be blank");
    }

    /**
     * 회원 존재 여부와 정규화된 조회 인자를 검증하기 위한 테스트용 fake다.
     */
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

    /**
     * 워크스페이스 조회 port 호출 여부와 인자를 캡처하는 테스트용 fake다.
     */
    private static class FakeLoadWorkspacePort implements LoadWorkspacePort {
        private final List<ListWorkspacesResult> results;
        private String requestedGoogleSub;
        private boolean called;

        private FakeLoadWorkspacePort(List<ListWorkspacesResult> results) {
            this.results = results;
        }

        @Override
        public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            this.called = true;
            this.requestedGoogleSub = googleSub;
            return results;
        }

        @Override
        public Optional<com.aquilaoffice.application.port.in.GetWorkspaceDetailResult>
                findJoinedWorkspaceByIdAndMemberGoogleSub(Long workspaceId, String googleSub) {
            throw new UnsupportedOperationException("not used in list service test");
        }
    }
}
