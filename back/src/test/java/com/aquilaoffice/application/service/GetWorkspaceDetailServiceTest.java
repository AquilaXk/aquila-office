package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 상세 조회 application service가 회원 검증과 상세 조회 계약을 지키는지 검증한다.
 */
class GetWorkspaceDetailServiceTest {

    @Test
    void getWorkspaceDetailReturnsJoinedWorkspaceDetail() {
        Member member = Member.createUser("google-1", "user@example.com", "User");
        FakeLoadMemberPort loadMemberPort = new FakeLoadMemberPort(Optional.of(member));
        FakeLoadWorkspacePort loadWorkspacePort = new FakeLoadWorkspacePort(Optional.of(new GetWorkspaceDetailResult(
                101L,
                "Team",
                "Description",
                "google-1",
                WorkspaceMemberRole.ADMIN,
                LocalDateTime.of(2026, 5, 15, 10, 0))));
        GetWorkspaceDetailService service = new GetWorkspaceDetailService(loadMemberPort, loadWorkspacePort);

        GetWorkspaceDetailResult result =
                service.getWorkspaceDetail(new GetWorkspaceDetailQuery(101L, " google-1 "));

        assertThat(loadMemberPort.requestedGoogleSub).isEqualTo("google-1");
        assertThat(loadWorkspacePort.requestedWorkspaceId).isEqualTo(101L);
        assertThat(loadWorkspacePort.requestedGoogleSub).isEqualTo("google-1");
        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.name()).isEqualTo("Team");
        assertThat(result.description()).isEqualTo("Description");
        assertThat(result.creatorGoogleSub()).isEqualTo("google-1");
        assertThat(result.myRole()).isEqualTo(WorkspaceMemberRole.ADMIN);
        assertThat(result.joinedAt()).isEqualTo(LocalDateTime.of(2026, 5, 15, 10, 0));
    }

    @Test
    void getWorkspaceDetailRejectsUnknownMemberAndDoesNotQueryWorkspace() {
        FakeLoadMemberPort loadMemberPort = new FakeLoadMemberPort(Optional.empty());
        FakeLoadWorkspacePort loadWorkspacePort = new FakeLoadWorkspacePort(Optional.empty());
        GetWorkspaceDetailService service = new GetWorkspaceDetailService(loadMemberPort, loadWorkspacePort);

        assertThatThrownBy(() -> service.getWorkspaceDetail(new GetWorkspaceDetailQuery(101L, "missing")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member not found");
        assertThat(loadWorkspacePort.called).isFalse();
    }

    @Test
    void getWorkspaceDetailRejectsMissingWorkspace() {
        Member member = Member.createUser("google-1", "user@example.com", "User");
        GetWorkspaceDetailService service = new GetWorkspaceDetailService(
                new FakeLoadMemberPort(Optional.of(member)),
                new FakeLoadWorkspacePort(Optional.empty()));

        assertThatThrownBy(() -> service.getWorkspaceDetail(new GetWorkspaceDetailQuery(101L, "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace not found");
    }

    @Test
    void queryRejectsInvalidWorkspaceId() {
        assertThatThrownBy(() -> new GetWorkspaceDetailQuery(0L, "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must be positive");
    }

    @Test
    void queryRejectsBlankGoogleSub() {
        assertThatThrownBy(() -> new GetWorkspaceDetailQuery(101L, " "))
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
            throw new UnsupportedOperationException("not used in detail service test");
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
}
