package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.DeleteWorkspaceCommand;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.out.DeleteWorkspacePort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.WorkspaceDeletion;
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
 * 워크스페이스 삭제 service가 관리자 멤버십을 확인한 뒤 soft delete 저장 port를 호출하는지 검증한다.
 */
class DeleteWorkspaceServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-05-15T04:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void deleteWorkspaceCallsPortWhenRequesterIsAdmin() {
        FakeDeleteWorkspacePort deleteWorkspacePort = new FakeDeleteWorkspacePort();
        DeleteWorkspaceService service = new DeleteWorkspaceService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                deleteWorkspacePort,
                CLOCK);

        service.deleteWorkspace(new DeleteWorkspaceCommand(101L, " google-1 "));

        assertThat(deleteWorkspacePort.deletion.workspaceId()).isEqualTo(101L);
        assertThat(deleteWorkspacePort.deletion.deletedAt())
                .isEqualTo(LocalDateTime.of(2026, 5, 15, 13, 0));
    }

    @Test
    void deleteWorkspaceRejectsNonAdminMembership() {
        FakeDeleteWorkspacePort deleteWorkspacePort = new FakeDeleteWorkspacePort();
        DeleteWorkspaceService service = new DeleteWorkspaceService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                deleteWorkspacePort,
                CLOCK);

        assertThatThrownBy(() -> service.deleteWorkspace(new DeleteWorkspaceCommand(101L, "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");
        assertThat(deleteWorkspacePort.called).isFalse();
    }

    @Test
    void commandRejectsInvalidRequiredValues() {
        assertThatThrownBy(() -> new DeleteWorkspaceCommand(0L, "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must be positive");
        assertThatThrownBy(() -> new DeleteWorkspaceCommand(101L, " "))
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

        private FakeLoadWorkspacePort(Optional<GetWorkspaceDetailResult> detailResult) {
            this.detailResult = detailResult;
        }

        @Override
        public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            throw new UnsupportedOperationException("not used in delete workspace service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId, String googleSub) {
            return detailResult;
        }
    }

    private static class FakeDeleteWorkspacePort implements DeleteWorkspacePort {
        private WorkspaceDeletion deletion;
        private boolean called;

        @Override
        public void delete(WorkspaceDeletion deletion) {
            this.called = true;
            this.deletion = deletion;
        }
    }
}
