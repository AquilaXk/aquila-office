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
import com.aquilaoffice.application.port.out.CreateWorkspaceAgentGatewayCommand;
import com.aquilaoffice.application.port.out.CreateWorkspaceAgentGatewayPort;
import com.aquilaoffice.application.port.out.CreateWorkspaceAgentGatewayResult;
import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgent;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgentSkillFile;
import com.aquilaoffice.domain.agent.WorkspaceAgent;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFile;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import java.nio.file.Files;
import java.nio.file.Path;
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
        CreateWorkspaceAgentService service = createAgentService(savePort, WorkspaceMemberRole.ADMIN);

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
        assertThat(savePort.savedSkillFiles)
                .extracting(WorkspaceAgentSkillFile::getFileName)
                .containsExactly("AGENTS.md", "backend-guide.md");
        assertThat(savePort.savedSkillFiles.getFirst().getFileName()).isEqualTo("AGENTS.md");
        assertThat(savePort.savedSkillFiles.getFirst().getContent()).isEqualTo("skill");
        assertThat(result.agentId()).isEqualTo(501L);
        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.status()).isEqualTo(WorkspaceAgentStatus.READY);
        assertThat(result.skillFiles()).hasSize(1);
    }

    @Test
    void createWorkspaceAgentAddsRoleTemplatesWhenSkillFilesAreEmpty() {
        FakeSaveWorkspaceAgentPort savePort = new FakeSaveWorkspaceAgentPort(savedAgent());
        CreateWorkspaceAgentService service = createAgentService(savePort, WorkspaceMemberRole.ADMIN);

        service.createWorkspaceAgent(new CreateWorkspaceAgentCommand(
                101L,
                "google-1",
                "Backend Agent",
                WorkspaceAgentCategory.BACKEND,
                ".aquila/workspaces/101",
                List.of()));

        assertThat(savePort.savedSkillFiles)
                .extracting(WorkspaceAgentSkillFile::getFileName)
                .containsExactly("AGENTS.md", "backend-guide.md");
        assertThat(savePort.savedSkillFiles.getFirst().getContent())
                .contains("aquila office", "작업 범위", "검증");
    }

    @Test
    void createWorkspaceAgentDefaultsCompatiblePayloadAndPassesEmojiToGateway() {
        FakeSaveWorkspaceAgentPort savePort = new FakeSaveWorkspaceAgentPort(null);
        FakeCreateWorkspaceAgentGatewayPort gatewayPort =
                new FakeCreateWorkspaceAgentGatewayPort(gatewayCreateSuccess());
        CreateWorkspaceAgentService service = new CreateWorkspaceAgentService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                savePort,
                new FakeLoadWorkspaceGatewayBindingPort(Optional.of(gatewayBinding())),
                gatewayPort);

        CreateWorkspaceAgentResult result = service.createWorkspaceAgent(new CreateWorkspaceAgentCommand(
                101L,
                "google-1",
                " QA Agent ",
                " 🧪 ",
                null,
                null,
                null));

        assertThat(savePort.savedAgent.getName()).isEqualTo("QA Agent");
        assertThat(savePort.savedAgent.getCategory()).isEqualTo(WorkspaceAgentCategory.CUSTOM);
        assertThat(savePort.savedAgent.getWorkspacePath()).isEqualTo(".aquila/workspaces/101");
        assertThat(gatewayPort.command.emoji()).isEqualTo("🧪");
        assertThat(gatewayPort.command.category()).isEqualTo(WorkspaceAgentCategory.CUSTOM);
        assertThat(gatewayPort.command.workspacePath()).isEqualTo(".aquila/workspaces/101");
        assertThat(savePort.savedSkillFiles)
                .extracting(WorkspaceAgentSkillFile::getSyncStatus)
                .containsExactly(WorkspaceAgentSkillFileSyncStatus.SYNCED);
        assertThat(result.status()).isEqualTo(WorkspaceAgentStatus.READY);
    }

    @Test
    void createWorkspaceAgentKeepsRequestedSkillFileWhenItOverridesDefault() {
        FakeSaveWorkspaceAgentPort savePort = new FakeSaveWorkspaceAgentPort(savedAgent());
        CreateWorkspaceAgentService service = createAgentService(savePort, WorkspaceMemberRole.ADMIN);

        service.createWorkspaceAgent(new CreateWorkspaceAgentCommand(
                101L,
                "google-1",
                "Frontend Agent",
                WorkspaceAgentCategory.FRONTEND,
                ".aquila/workspaces/101",
                List.of(new WorkspaceAgentSkillFileCommand(" AGENTS.md ", "custom agents"))));

        assertThat(savePort.savedSkillFiles)
                .extracting(WorkspaceAgentSkillFile::getFileName)
                .containsExactly("AGENTS.md", "frontend-guide.md");
        assertThat(savePort.savedSkillFiles.getFirst().getContent()).isEqualTo("custom agents");
    }

    @Test
    void vertexLocalKeyIsIgnoredAndEnvSamplesExposeModelSettings() throws Exception {
        Path root = Path.of(System.getProperty("user.dir")).getParent();

        assertThat(Files.readString(root.resolve(".gitignore"))).contains("vertex-key.json");
        assertThat(Files.readString(root.resolve(".env.example")))
                .contains("GOOGLE_CLOUD_PROJECT_ID=grepp-ai-project")
                .contains("VERTEX_AI_LOCATION=us-central1")
                .contains("VERTEX_AI_MODEL=gemini-2.5-flash")
                .contains("VERTEX_AI_CREDENTIALS_HOST_PATH=./vertex-key.json")
                .contains("GOOGLE_APPLICATION_CREDENTIALS=/run/secrets/vertex-key.json");
        assertThat(Files.readString(root.resolve("back/.env.example")))
                .contains("GOOGLE_CLOUD_PROJECT_ID=grepp-ai-project")
                .contains("VERTEX_AI_LOCATION=us-central1")
                .contains("VERTEX_AI_MODEL=gemini-2.5-flash")
                .contains("GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/aquila-office/vertex-key.json");
    }

    @Test
    void createWorkspaceAgentRequiresActiveGatewayBindingBeforeSave() {
        FakeSaveWorkspaceAgentPort savePort = new FakeSaveWorkspaceAgentPort(null);
        FakeCreateWorkspaceAgentGatewayPort gatewayPort =
                new FakeCreateWorkspaceAgentGatewayPort(gatewayCreateSuccess());
        CreateWorkspaceAgentService service = new CreateWorkspaceAgentService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                savePort,
                new FakeLoadWorkspaceGatewayBindingPort(Optional.empty()),
                gatewayPort);

        assertThatThrownBy(() -> service.createWorkspaceAgent(new CreateWorkspaceAgentCommand(
                        101L,
                        "google-1",
                        "Backend Agent",
                        WorkspaceAgentCategory.BACKEND,
                        ".aquila/workspaces/101",
                        List.of())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("workspace gateway binding required");
        assertThat(savePort.called).isFalse();
        assertThat(gatewayPort.called).isFalse();
    }

    @Test
    void createWorkspaceAgentMarksReadyWithGatewayAgentId() {
        FakeSaveWorkspaceAgentPort savePort = new FakeSaveWorkspaceAgentPort(null);
        FakeCreateWorkspaceAgentGatewayPort gatewayPort =
                new FakeCreateWorkspaceAgentGatewayPort(gatewayCreateSuccess());
        CreateWorkspaceAgentService service = new CreateWorkspaceAgentService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                savePort,
                new FakeLoadWorkspaceGatewayBindingPort(Optional.of(gatewayBinding())),
                gatewayPort);

        CreateWorkspaceAgentResult result = service.createWorkspaceAgent(new CreateWorkspaceAgentCommand(
                101L,
                "google-1",
                "Backend Agent",
                WorkspaceAgentCategory.BACKEND,
                ".aquila/workspaces/101",
                List.of()));

        assertThat(gatewayPort.command.workspaceId()).isEqualTo(101L);
        assertThat(gatewayPort.command.gatewayUrl()).isEqualTo("https://gateway.example.com");
        assertThat(gatewayPort.command.gatewayToken()).isEqualTo("gate_1234567890abcdef");
        assertThat(gatewayPort.command.skillFiles())
                .extracting(CreateWorkspaceAgentGatewayCommand.SkillFile::fileName)
                .containsExactly("AGENTS.md", "backend-guide.md");
        assertThat(savePort.savedSkillFiles)
                .extracting(WorkspaceAgentSkillFile::getSyncStatus)
                .containsExactly(
                        WorkspaceAgentSkillFileSyncStatus.SYNCED,
                        WorkspaceAgentSkillFileSyncStatus.SYNCED);
        assertThat(savePort.savedAgent.getStatus()).isEqualTo(WorkspaceAgentStatus.READY);
        assertThat(savePort.savedAgent.getOpenClawAgentId()).isEqualTo("oc-agent-1");
        assertThat(result.status()).isEqualTo(WorkspaceAgentStatus.READY);
        assertThat(result.openClawAgentId()).isEqualTo("oc-agent-1");
    }

    @Test
    void createWorkspaceAgentRecordsFailedSkillFileSync() {
        FakeSaveWorkspaceAgentPort savePort = new FakeSaveWorkspaceAgentPort(null);
        CreateWorkspaceAgentService service = new CreateWorkspaceAgentService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                savePort,
                new FakeLoadWorkspaceGatewayBindingPort(Optional.of(gatewayBinding())),
                new FakeCreateWorkspaceAgentGatewayPort(new CreateWorkspaceAgentGatewayResult(
                        false,
                        "oc-agent-1",
                        "gateway skill file sync failed",
                        List.of(
                                new CreateWorkspaceAgentGatewayResult.SkillFileResult("AGENTS.md", true, null),
                                new CreateWorkspaceAgentGatewayResult.SkillFileResult(
                                        "backend-guide.md",
                                        false,
                                        "write denied")))));

        CreateWorkspaceAgentResult result = service.createWorkspaceAgent(new CreateWorkspaceAgentCommand(
                101L,
                "google-1",
                "Backend Agent",
                WorkspaceAgentCategory.BACKEND,
                ".aquila/workspaces/101",
                List.of()));

        assertThat(savePort.savedAgent.getStatus()).isEqualTo(WorkspaceAgentStatus.SYNC_FAILED);
        assertThat(savePort.savedAgent.getOpenClawAgentId()).isEqualTo("oc-agent-1");
        assertThat(savePort.savedAgent.getSyncError()).isEqualTo("gateway skill file sync failed");
        assertThat(savePort.savedSkillFiles)
                .extracting(WorkspaceAgentSkillFile::getSyncStatus)
                .containsExactly(
                        WorkspaceAgentSkillFileSyncStatus.SYNCED,
                        WorkspaceAgentSkillFileSyncStatus.FAILED);
        assertThat(savePort.savedSkillFiles.get(1).getSyncError()).isEqualTo("write denied");
        assertThat(result.status()).isEqualTo(WorkspaceAgentStatus.SYNC_FAILED);
    }

    @Test
    void createWorkspaceAgentMarksSyncFailedWhenGatewayCreateFails() {
        FakeSaveWorkspaceAgentPort savePort = new FakeSaveWorkspaceAgentPort(null);
        CreateWorkspaceAgentService service = new CreateWorkspaceAgentService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                savePort,
                new FakeLoadWorkspaceGatewayBindingPort(Optional.of(gatewayBinding())),
                new FakeCreateWorkspaceAgentGatewayPort(
                        new CreateWorkspaceAgentGatewayResult(false, null, "gateway unavailable")));

        CreateWorkspaceAgentResult result = service.createWorkspaceAgent(new CreateWorkspaceAgentCommand(
                101L,
                "google-1",
                "Backend Agent",
                WorkspaceAgentCategory.BACKEND,
                ".aquila/workspaces/101",
                List.of()));

        assertThat(savePort.savedAgent.getStatus()).isEqualTo(WorkspaceAgentStatus.SYNC_FAILED);
        assertThat(savePort.savedAgent.getOpenClawAgentId()).isNull();
        assertThat(savePort.savedAgent.getSyncError()).isEqualTo("gateway unavailable");
        assertThat(result.status()).isEqualTo(WorkspaceAgentStatus.SYNC_FAILED);
        assertThat(result.syncError()).isEqualTo("gateway unavailable");
    }

    @Test
    void createWorkspaceAgentRejectsNonAdmin() {
        FakeSaveWorkspaceAgentPort savePort = new FakeSaveWorkspaceAgentPort(savedAgent());
        FakeCreateWorkspaceAgentGatewayPort gatewayPort =
                new FakeCreateWorkspaceAgentGatewayPort(gatewayCreateSuccess());
        CreateWorkspaceAgentService service = new CreateWorkspaceAgentService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                savePort,
                new FakeLoadWorkspaceGatewayBindingPort(Optional.of(gatewayBinding())),
                gatewayPort);

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
        assertThat(gatewayPort.called).isFalse();
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

    private static CreateWorkspaceAgentService createAgentService(
            FakeSaveWorkspaceAgentPort savePort,
            WorkspaceMemberRole role) {
        return new CreateWorkspaceAgentService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(role))),
                savePort,
                new FakeLoadWorkspaceGatewayBindingPort(Optional.of(gatewayBinding())),
                new FakeCreateWorkspaceAgentGatewayPort(gatewayCreateSuccess()));
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

    private static WorkspaceGatewayBinding gatewayBinding() {
        return WorkspaceGatewayBinding.restore(
                701L,
                101L,
                "https://gateway.example.com",
                "gate_1234567890abcdef",
                "google-1");
    }

    private static CreateWorkspaceAgentGatewayResult gatewayCreateSuccess() {
        return new CreateWorkspaceAgentGatewayResult(true, "oc-agent-1", null);
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
            if (result != null) {
                return result;
            }
            return new SavedWorkspaceAgent(
                    501L,
                    agent.getWorkspaceId(),
                    agent.getName(),
                    agent.getCategory(),
                    agent.getOpenClawAgentId(),
                    agent.getWorkspacePath(),
                    agent.getStatus(),
                    agent.getSyncError(),
                    agent.getCreatedByGoogleSub(),
                    LocalDateTime.of(2026, 5, 16, 10, 0),
                    LocalDateTime.of(2026, 5, 16, 10, 0),
                    skillFiles.stream()
                            .map(skillFile -> new SavedWorkspaceAgentSkillFile(
                                    601L,
                                    skillFile.getFileName(),
                                    skillFile.getSyncStatus(),
                                    skillFile.getSyncError()))
                            .toList());
        }
    }

    private static class FakeLoadWorkspaceGatewayBindingPort implements LoadWorkspaceGatewayBindingPort {
        private final Optional<WorkspaceGatewayBinding> binding;

        private FakeLoadWorkspaceGatewayBindingPort(Optional<WorkspaceGatewayBinding> binding) {
            this.binding = binding;
        }

        @Override
        public Optional<WorkspaceGatewayBinding> findActiveByWorkspaceId(Long workspaceId) {
            return binding;
        }
    }

    private static class FakeCreateWorkspaceAgentGatewayPort implements CreateWorkspaceAgentGatewayPort {
        private final CreateWorkspaceAgentGatewayResult result;
        private CreateWorkspaceAgentGatewayCommand command;
        private boolean called;

        private FakeCreateWorkspaceAgentGatewayPort(CreateWorkspaceAgentGatewayResult result) {
            this.result = result;
        }

        @Override
        public CreateWorkspaceAgentGatewayResult create(CreateWorkspaceAgentGatewayCommand command) {
            this.called = true;
            this.command = command;
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
