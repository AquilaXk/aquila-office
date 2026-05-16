package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.CreateWorkspaceAgentCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.DeleteWorkspaceAgentCommand;
import com.aquilaoffice.application.port.in.GetWorkspaceAgentQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.ListWorkspaceAgentsQuery;
import com.aquilaoffice.application.port.in.ListWorkspaceAgentsResult;
import com.aquilaoffice.application.port.in.WorkspaceAgentSkillFileCommand;
import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgent;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgentSkillFile;
import com.aquilaoffice.domain.agent.WorkspaceAgent;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFile;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Agent application service가 임시 회원 헤더와 워크스페이스 멤버십을 기준으로 권한을 검증하는지 확인한다.
 */
class WorkspaceAgentServiceTest {

    @Test
    void createWorkspaceAgentRequiresAdminAndSavesSkillFiles() {
        FakeSaveWorkspaceAgentPort savePort = new FakeSaveWorkspaceAgentPort(savedAgent());
        CreateWorkspaceAgentService service = new CreateWorkspaceAgentService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                savePort);

        CreateWorkspaceAgentResult result = service.createWorkspaceAgent(new CreateWorkspaceAgentCommand(
                101L,
                " google-1 ",
                " Backend Agent ",
                WorkspaceAgentCategory.BACKEND,
                " ",
                List.of(new WorkspaceAgentSkillFileCommand(" AGENTS.md ", "skill"))));

        assertThat(savePort.savedAgent.getWorkspaceId()).isEqualTo(101L);
        assertThat(savePort.savedAgent.getName()).isEqualTo("Backend Agent");
        assertThat(savePort.savedAgent.getCategory()).isEqualTo(WorkspaceAgentCategory.BACKEND);
        assertThat(savePort.savedAgent.getWorkspacePath()).isEqualTo(".aquila/workspaces/101");
        assertThat(savePort.savedSkillFiles).hasSize(1);
        assertThat(savePort.savedSkillFiles.getFirst().getFileName()).isEqualTo("AGENTS.md");
        assertThat(result.agentId()).isEqualTo(501L);
        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.status()).isEqualTo(WorkspaceAgentStatus.READY);
        assertThat(result.skillFiles()).hasSize(1);
    }

    @Test
    void createWorkspaceAgentRejectsNonAdmin() {
        FakeSaveWorkspaceAgentPort savePort = new FakeSaveWorkspaceAgentPort(savedAgent());
        CreateWorkspaceAgentService service = new CreateWorkspaceAgentService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                savePort);

        assertThatThrownBy(() -> service.createWorkspaceAgent(new CreateWorkspaceAgentCommand(
                        101L,
                        "google-1",
                        "Backend Agent",
                        WorkspaceAgentCategory.BACKEND,
                        ".aquila/workspaces/101",
                        List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");
        assertThat(savePort.called).isFalse();
    }

    @Test
    void listWorkspaceAgentsReturnsJoinedWorkspaceAgents() {
        ListWorkspaceAgentsService service = new ListWorkspaceAgentsService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-2", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                new FakeLoadWorkspaceAgentPort(List.of(savedAgent()), Optional.empty()));

        List<ListWorkspaceAgentsResult> results = service.listWorkspaceAgents(
                new ListWorkspaceAgentsQuery(101L, " google-2 "));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().agentId()).isEqualTo(501L);
        assertThat(results.getFirst().name()).isEqualTo("Backend Agent");
    }

    @Test
    void getWorkspaceAgentReturnsJoinedWorkspaceAgent() {
        GetWorkspaceAgentService service = new GetWorkspaceAgentService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-2", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                new FakeLoadWorkspaceAgentPort(List.of(), Optional.of(savedAgent())));

        GetWorkspaceAgentResult result = service.getWorkspaceAgent(new GetWorkspaceAgentQuery(101L, 501L, "google-2"));

        assertThat(result.agentId()).isEqualTo(501L);
        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.skillFiles().getFirst().syncStatus()).isEqualTo(WorkspaceAgentSkillFileSyncStatus.SYNCED);
    }

    @Test
    void deleteWorkspaceAgentRequiresAdminAndDisablesAgent() {
        FakeDeleteWorkspaceAgentPort deletePort = new FakeDeleteWorkspaceAgentPort();
        DeleteWorkspaceAgentService service = new DeleteWorkspaceAgentService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                deletePort);

        service.deleteWorkspaceAgent(new DeleteWorkspaceAgentCommand(101L, 501L, " google-1 "));

        assertThat(deletePort.disabledWorkspaceId).isEqualTo(101L);
        assertThat(deletePort.disabledAgentId).isEqualTo(501L);
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

    private static SavedWorkspaceAgent savedAgent() {
        return new SavedWorkspaceAgent(
                501L,
                101L,
                "Backend Agent",
                WorkspaceAgentCategory.BACKEND,
                "gateway-agent-1",
                ".aquila/workspaces/101",
                WorkspaceAgentStatus.READY,
                null,
                "google-1",
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0),
                List.of(new SavedWorkspaceAgentSkillFile(
                        601L,
                        "AGENTS.md",
                        WorkspaceAgentSkillFileSyncStatus.SYNCED,
                        null)));
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
        private final Optional<GetWorkspaceDetailResult> detail;

        private FakeLoadWorkspacePort(Optional<GetWorkspaceDetailResult> detail) {
            this.detail = detail;
        }

        @Override
        public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            throw new UnsupportedOperationException("not used in agent service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId,
                String googleSub) {
            return detail;
        }
    }

    private static class FakeSaveWorkspaceAgentPort implements SaveWorkspaceAgentPort {
        private final SavedWorkspaceAgent result;
        private WorkspaceAgent savedAgent;
        private List<WorkspaceAgentSkillFile> savedSkillFiles;
        private boolean called;

        private FakeSaveWorkspaceAgentPort(SavedWorkspaceAgent result) {
            this.result = result;
        }

        @Override
        public SavedWorkspaceAgent save(WorkspaceAgent agent, List<WorkspaceAgentSkillFile> skillFiles) {
            this.called = true;
            this.savedAgent = agent;
            this.savedSkillFiles = skillFiles;
            return result;
        }
    }

    private static class FakeLoadWorkspaceAgentPort implements LoadWorkspaceAgentPort {
        private final List<SavedWorkspaceAgent> agents;
        private final Optional<SavedWorkspaceAgent> agent;

        private FakeLoadWorkspaceAgentPort(List<SavedWorkspaceAgent> agents, Optional<SavedWorkspaceAgent> agent) {
            this.agents = agents;
            this.agent = agent;
        }

        @Override
        public List<SavedWorkspaceAgent> findActiveByWorkspaceId(Long workspaceId) {
            return agents;
        }

        @Override
        public Optional<SavedWorkspaceAgent> findActiveByWorkspaceIdAndAgentId(Long workspaceId, Long agentId) {
            return agent;
        }
    }

    private static class FakeDeleteWorkspaceAgentPort implements DeleteWorkspaceAgentPort {
        private Long disabledWorkspaceId;
        private Long disabledAgentId;

        @Override
        public void disable(Long workspaceId, Long agentId) {
            this.disabledWorkspaceId = workspaceId;
            this.disabledAgentId = agentId;
        }
    }
}
