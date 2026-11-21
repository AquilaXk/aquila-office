package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.UpdateWorkspaceCommand;
import com.aquilaoffice.application.port.in.UpdateWorkspaceResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.UpdateWorkspacePort;
import com.aquilaoffice.application.port.out.UpdatedWorkspace;
import com.aquilaoffice.application.port.out.WorkspaceUpdate;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 정보 수정 service가 관리자 멤버십을 확인한 뒤 저장 port를 호출하는지 검증한다.
 */
class UpdateWorkspaceServiceTest {

    @Test
    void updateWorkspaceReturnsUpdatedWorkspaceWhenRequesterIsAdmin() {
        Member requester = Member.createUser("google-1", "admin@example.com", "Admin");
        FakeUpdateWorkspacePort updateWorkspacePort = new FakeUpdateWorkspacePort(new UpdatedWorkspace(
                101L,
                "Updated Team",
                "Updated Description",
                "google-1"));
        UpdateWorkspaceService service = new UpdateWorkspaceService(
                new FakeLoadMemberPort(Optional.of(requester)),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                updateWorkspacePort);

        UpdateWorkspaceResult result = service.updateWorkspace(new UpdateWorkspaceCommand(
                101L,
                " google-1 ",
                " Updated Team ",
                " Updated Description "));

        assertThat(updateWorkspacePort.update.workspaceId()).isEqualTo(101L);
        assertThat(updateWorkspacePort.update.name()).isEqualTo("Updated Team");
        assertThat(updateWorkspacePort.update.description()).isEqualTo(" Updated Description ");
        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.name()).isEqualTo("Updated Team");
        assertThat(result.description()).isEqualTo("Updated Description");
        assertThat(result.creatorGoogleSub()).isEqualTo("google-1");
        assertThat(result.myRole()).isEqualTo(WorkspaceMemberRole.ADMIN);
        assertThat(result.joinedAt()).isEqualTo(LocalDateTime.of(2026, 5, 15, 10, 0));
    }

    @Test
    void updateWorkspacePassesAndNormalizesDailyTokenLimit() {
        Member requester = Member.createUser("google-1", "admin@example.com", "Admin");
        FakeUpdateWorkspacePort updateWorkspacePort = new FakeUpdateWorkspacePort(new UpdatedWorkspace(
                101L,
                "Updated Team",
                "Updated Description",
                "google-1",
                50_000));
        UpdateWorkspaceService service = new UpdateWorkspaceService(
                new FakeLoadMemberPort(Optional.of(requester)),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                updateWorkspacePort);

        UpdateWorkspaceResult result = service.updateWorkspace(new UpdateWorkspaceCommand(
                101L,
                "google-1",
                "Updated Team",
                "Updated Description",
                50_000));

        assertThat(updateWorkspacePort.update.dailyTokenLimit()).isEqualTo(50_000);
        assertThat(result.dailyTokenLimit()).isEqualTo(50_000);
        assertThat(new UpdateWorkspaceCommand(101L, "google-1", "Team", null, 0).dailyTokenLimit()).isNull();
        assertThat(new UpdateWorkspaceCommand(101L, "google-1", "Team", null, -5).dailyTokenLimit()).isNull();
    }

    @Test
    void updateWorkspaceRejectsNonAdminMembership() {
        FakeUpdateWorkspacePort updateWorkspacePort = new FakeUpdateWorkspacePort(null);
        UpdateWorkspaceService service = new UpdateWorkspaceService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                updateWorkspacePort);

        assertThatThrownBy(() -> service.updateWorkspace(new UpdateWorkspaceCommand(
                        101L,
                        "google-1",
                        "Updated Team",
                        null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");
        assertThat(updateWorkspacePort.called).isFalse();
    }

    @Test
    void updateWorkspaceRejectsUnknownRequesterAndDoesNotQueryWorkspace() {
        FakeLoadWorkspacePort loadWorkspacePort = new FakeLoadWorkspacePort(Optional.empty());
        FakeUpdateWorkspacePort updateWorkspacePort = new FakeUpdateWorkspacePort(null);
        UpdateWorkspaceService service = new UpdateWorkspaceService(
                new FakeLoadMemberPort(Optional.empty()), loadWorkspacePort, updateWorkspacePort);

        assertThatThrownBy(() -> service.updateWorkspace(new UpdateWorkspaceCommand(
                        101L,
                        "missing-google",
                        "Updated Team",
                        null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member not found");
        assertThat(loadWorkspacePort.called).isFalse();
        assertThat(updateWorkspacePort.called).isFalse();
    }

    @Test
    void commandRejectsInvalidRequiredValues() {
        assertThatThrownBy(() -> new UpdateWorkspaceCommand(0L, "google-1", "Team", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must be positive");
        assertThatThrownBy(() -> new UpdateWorkspaceCommand(101L, " ", "Team", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requesterGoogleSub must not be blank");
        assertThatThrownBy(() -> new UpdateWorkspaceCommand(101L, "google-1", " ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
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
            throw new UnsupportedOperationException("not used in update workspace service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId, String googleSub) {
            this.called = true;
            return detailResult;
        }
    }

    private static class FakeUpdateWorkspacePort implements UpdateWorkspacePort {
        private final UpdatedWorkspace updatedWorkspace;
        private WorkspaceUpdate update;
        private boolean called;

        private FakeUpdateWorkspacePort(UpdatedWorkspace updatedWorkspace) {
            this.updatedWorkspace = updatedWorkspace;
        }

        @Override
        public UpdatedWorkspace update(WorkspaceUpdate update) {
            this.called = true;
            this.update = update;
            return updatedWorkspace;
        }
    }
}
