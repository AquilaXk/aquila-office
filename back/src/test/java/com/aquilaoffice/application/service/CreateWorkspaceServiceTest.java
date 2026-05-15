package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.CreateWorkspaceCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.SaveWorkspacePort;
import com.aquilaoffice.application.port.out.SavedWorkspace;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.Workspace;
import com.aquilaoffice.domain.workspace.WorkspaceMember;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreateWorkspaceServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-05-15T01:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void createWorkspaceBuildsWorkspaceAndAdminMembership() {
        Member creator = Member.createUser("google-1", "user@example.com", "User");
        FakeLoadMemberPort loadMemberPort = new FakeLoadMemberPort(Optional.of(creator));
        FakeSaveWorkspacePort saveWorkspacePort = new FakeSaveWorkspacePort(new SavedWorkspace(101L));
        CreateWorkspaceService service = new CreateWorkspaceService(loadMemberPort, saveWorkspacePort, CLOCK);

        CreateWorkspaceResult result =
                service.createWorkspace(new CreateWorkspaceCommand(" Team ", " Description ", " google-1 "));

        assertThat(loadMemberPort.requestedGoogleSub).isEqualTo("google-1");
        assertThat(saveWorkspacePort.savedWorkspace.getName()).isEqualTo("Team");
        assertThat(saveWorkspacePort.savedWorkspace.getDescription()).isEqualTo("Description");
        assertThat(saveWorkspacePort.savedWorkspace.getCreatedByMember()).isSameAs(creator);
        assertThat(saveWorkspacePort.savedWorkspaceMember.getWorkspace()).isSameAs(saveWorkspacePort.savedWorkspace);
        assertThat(saveWorkspacePort.savedWorkspaceMember.getMember()).isSameAs(creator);
        assertThat(saveWorkspacePort.savedWorkspaceMember.getRole()).isEqualTo(WorkspaceMemberRole.ADMIN);
        assertThat(saveWorkspacePort.savedWorkspaceMember.getJoinedAt())
                .isEqualTo(LocalDateTime.of(2026, 5, 15, 10, 0));

        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.name()).isEqualTo("Team");
        assertThat(result.description()).isEqualTo("Description");
        assertThat(result.creatorGoogleSub()).isEqualTo("google-1");
        assertThat(result.myRole()).isEqualTo(WorkspaceMemberRole.ADMIN);
        assertThat(result.joinedAt()).isEqualTo(LocalDateTime.of(2026, 5, 15, 10, 0));
    }

    @Test
    void createWorkspaceRejectsUnknownCreatorAndDoesNotSave() {
        FakeLoadMemberPort loadMemberPort = new FakeLoadMemberPort(Optional.empty());
        FakeSaveWorkspacePort saveWorkspacePort = new FakeSaveWorkspacePort(new SavedWorkspace(101L));
        CreateWorkspaceService service = new CreateWorkspaceService(loadMemberPort, saveWorkspacePort, CLOCK);

        assertThatThrownBy(() ->
                        service.createWorkspace(new CreateWorkspaceCommand("Team", null, "google-missing")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("creator member not found");
        assertThat(saveWorkspacePort.called).isFalse();
    }

    @Test
    void commandRejectsBlankCreatorGoogleSub() {
        assertThatThrownBy(() -> new CreateWorkspaceCommand("Team", null, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("creatorGoogleSub must not be blank");
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

    private static class FakeSaveWorkspacePort implements SaveWorkspacePort {
        private final SavedWorkspace savedWorkspaceResult;
        private Workspace savedWorkspace;
        private WorkspaceMember savedWorkspaceMember;
        private boolean called;

        private FakeSaveWorkspacePort(SavedWorkspace savedWorkspaceResult) {
            this.savedWorkspaceResult = savedWorkspaceResult;
        }

        @Override
        public SavedWorkspace save(Workspace workspace, WorkspaceMember workspaceMember) {
            this.called = true;
            this.savedWorkspace = workspace;
            this.savedWorkspaceMember = workspaceMember;
            return savedWorkspaceResult;
        }
    }
}
