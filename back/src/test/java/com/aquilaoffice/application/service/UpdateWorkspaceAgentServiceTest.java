package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.UpdateWorkspaceAgentCommand;
import com.aquilaoffice.application.port.in.UpdateWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.WorkspaceAgentSkillFileCommand;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgent;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgentSkillFile;
import com.aquilaoffice.application.port.out.SyncWorkspaceAgentSkillFilesCommand;
import com.aquilaoffice.application.port.out.SyncWorkspaceAgentSkillFilesGatewayPort;
import com.aquilaoffice.application.port.out.SyncWorkspaceAgentSkillFilesResult;
import com.aquilaoffice.application.port.out.UpdateWorkspaceAgentPort;
import com.aquilaoffice.domain.agent.WorkspaceAgent;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFile;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Agent 수정 service의 관리자 권한, 입력 상한, Gateway skill 동기화 규칙을 검증한다.
 */
class UpdateWorkspaceAgentServiceTest {

    @Test
    void adminUpdatesAgentAndSyncsSkillFilesWhenGatewayBound() {
        FakeLoadMemberPort memberPort =
                new FakeLoadMemberPort(Optional.of(Member.createUser("admin-1", "admin@example.com", "Admin")));
        FakeLoadWorkspacePort workspacePort = new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN)));
        FakeAgentStore agentStore = new FakeAgentStore(existingAgent("oc-agent-1", WorkspaceAgentStatus.READY));
        FakeLoadGatewayBindingPort gatewayBindingPort = new FakeLoadGatewayBindingPort(Optional.of(
                WorkspaceGatewayBinding.create(101L, "https://gateway.example.com", "secret-token", "admin-1")));
        FakeSyncGatewayPort syncGatewayPort = new FakeSyncGatewayPort(new SyncWorkspaceAgentSkillFilesResult(
                true,
                null,
                List.of(new SyncWorkspaceAgentSkillFilesResult.SkillFileResult("AGENTS.md", true, null))));

        UpdateWorkspaceAgentService service = new UpdateWorkspaceAgentService(
                memberPort,
                workspacePort,
                agentStore,
                agentStore,
                gatewayBindingPort,
                syncGatewayPort);

        UpdateWorkspaceAgentResult result = service.updateWorkspaceAgent(new UpdateWorkspaceAgentCommand(
                101L,
                501L,
                "admin-1",
                "QA Agent",
                WorkspaceAgentCategory.QA,
                ".aquila/workspaces/qa",
                List.of(new WorkspaceAgentSkillFileCommand("AGENTS.md", "# updated qa rules"))));

        assertThat(result.name()).isEqualTo("QA Agent");
        assertThat(result.category()).isEqualTo(WorkspaceAgentCategory.QA);
        assertThat(result.workspacePath()).isEqualTo(".aquila/workspaces/qa");
        assertThat(result.status()).isEqualTo(WorkspaceAgentStatus.READY);
        assertThat(syncGatewayPort.called).isTrue();
        assertThat(syncGatewayPort.lastCommand.openClawAgentId()).isEqualTo("oc-agent-1");
        assertThat(syncGatewayPort.lastCommand.skillFiles())
                .containsExactly(new SyncWorkspaceAgentSkillFilesCommand.SkillFile("AGENTS.md", "# updated qa rules"));
        assertThat(result.skillFiles()).singleElement().satisfies(file -> {
            assertThat(file.fileName()).isEqualTo("AGENTS.md");
            assertThat(file.content()).isEqualTo("# updated qa rules");
            assertThat(file.syncStatus()).isEqualTo(WorkspaceAgentSkillFileSyncStatus.SYNCED);
        });
    }

    @Test
    void updatesLocallyWithoutCallingGatewayWhenNoBindingExists() {
        FakeLoadMemberPort memberPort =
                new FakeLoadMemberPort(Optional.of(Member.createUser("admin-1", "admin@example.com", "Admin")));
        FakeLoadWorkspacePort workspacePort = new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN)));
        FakeAgentStore agentStore = new FakeAgentStore(existingAgent(null, WorkspaceAgentStatus.CREATING));
        FakeLoadGatewayBindingPort gatewayBindingPort = new FakeLoadGatewayBindingPort(Optional.empty());
        FakeSyncGatewayPort syncGatewayPort = new FakeSyncGatewayPort(null);

        UpdateWorkspaceAgentService service = new UpdateWorkspaceAgentService(
                memberPort,
                workspacePort,
                agentStore,
                agentStore,
                gatewayBindingPort,
                syncGatewayPort);

        UpdateWorkspaceAgentResult result = service.updateWorkspaceAgent(new UpdateWorkspaceAgentCommand(
                101L,
                501L,
                "admin-1",
                "Local Agent",
                WorkspaceAgentCategory.BACKEND,
                ".aquila/workspaces/local",
                List.of(new WorkspaceAgentSkillFileCommand("GUIDE.md", "local guide"))));

        assertThat(syncGatewayPort.called).isFalse();
        assertThat(result.skillFiles()).singleElement().satisfies(file -> {
            assertThat(file.fileName()).isEqualTo("GUIDE.md");
            assertThat(file.content()).isEqualTo("local guide");
            assertThat(file.syncStatus()).isEqualTo(WorkspaceAgentSkillFileSyncStatus.PENDING);
        });
    }

    @Test
    void rejectsNonAdminMemberAndLeavesAgentUntouched() {
        FakeLoadMemberPort memberPort =
                new FakeLoadMemberPort(Optional.of(Member.createUser("member-1", "member@example.com", "Member")));
        FakeLoadWorkspacePort workspacePort =
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER)));
        FakeAgentStore agentStore = new FakeAgentStore(existingAgent("oc-agent-1", WorkspaceAgentStatus.READY));
        FakeLoadGatewayBindingPort gatewayBindingPort = new FakeLoadGatewayBindingPort(Optional.empty());
        FakeSyncGatewayPort syncGatewayPort = new FakeSyncGatewayPort(null);

        UpdateWorkspaceAgentService service = new UpdateWorkspaceAgentService(
                memberPort,
                workspacePort,
                agentStore,
                agentStore,
                gatewayBindingPort,
                syncGatewayPort);

        assertThatThrownBy(() -> service.updateWorkspaceAgent(new UpdateWorkspaceAgentCommand(
                        101L,
                        501L,
                        "member-1",
                        "Updated",
                        WorkspaceAgentCategory.BACKEND,
                        null,
                        null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");
        assertThat(agentStore.updateCalled).isFalse();
        assertThat(syncGatewayPort.called).isFalse();
    }

    @Test
    void rejectsMaliciousSkillFileInputs() {
        List<WorkspaceAgentSkillFileCommand> tooMany = new ArrayList<>();
        for (int i = 0; i <= WorkspaceAgentSkillFile.MAX_SKILL_FILES; i++) {
            tooMany.add(new WorkspaceAgentSkillFileCommand("FILE_" + i + ".md", "ok"));
        }

        assertThatThrownBy(() -> new UpdateWorkspaceAgentCommand(
                        101L,
                        501L,
                        "admin-1",
                        "Agent",
                        WorkspaceAgentCategory.BACKEND,
                        null,
                        tooMany))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("skillFiles must not exceed 20 entries");

        assertThatThrownBy(() -> new WorkspaceAgentSkillFileCommand("../secret.md", "ok"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("fileName contains unsafe path characters");

        assertThatThrownBy(() -> new WorkspaceAgentSkillFileCommand("sub/AGENTS.md", "ok"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("fileName contains unsafe path characters");

        String oversizedContent = "a".repeat(WorkspaceAgentSkillFile.MAX_CONTENT_LENGTH + 1);
        assertThatThrownBy(() -> new WorkspaceAgentSkillFileCommand("AGENTS.md", oversizedContent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("skill file content exceeds max length");
    }

    @Test
    void clearsModelConfigWhenUpdateModelConfigFlagIsTrue() {
        FakeLoadMemberPort memberPort =
                new FakeLoadMemberPort(Optional.of(Member.createUser("admin-1", "admin@example.com", "Admin")));
        FakeLoadWorkspacePort workspacePort = new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN)));
        SavedWorkspaceAgent agentWithModel = new SavedWorkspaceAgent(
                501L,
                101L,
                "Model Agent",
                WorkspaceAgentCategory.BACKEND,
                "direct-model:abc",
                ".aquila/workspaces/old",
                WorkspaceAgentStatus.READY,
                null,
                "admin-1",
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0),
                List.of(),
                9L,
                "gpt-5.2",
                "claude-sonnet-4-5");
        FakeAgentStore agentStore = new FakeAgentStore(agentWithModel);
        UpdateWorkspaceAgentService service = new UpdateWorkspaceAgentService(
                memberPort,
                workspacePort,
                agentStore,
                agentStore,
                new FakeLoadGatewayBindingPort(Optional.empty()),
                new FakeSyncGatewayPort(null));

        UpdateWorkspaceAgentResult result = service.updateWorkspaceAgent(new UpdateWorkspaceAgentCommand(
                101L,
                501L,
                "admin-1",
                "Model Agent",
                WorkspaceAgentCategory.BACKEND,
                null,
                null,
                null,
                null,
                null,
                true));

        assertThat(result.modelProviderId()).isNull();
        assertThat(result.modelId()).isNull();
        assertThat(result.fallbackModelId()).isNull();
    }

    @Test
    void skipsGatewaySkillSyncForDirectModelAgentsEvenWhenGatewayBound() {
        FakeLoadMemberPort memberPort =
                new FakeLoadMemberPort(Optional.of(Member.createUser("admin-1", "admin@example.com", "Admin")));
        FakeLoadWorkspacePort workspacePort = new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN)));
        FakeAgentStore agentStore = new FakeAgentStore(existingAgent("direct-model:uuid-1", WorkspaceAgentStatus.READY));
        FakeLoadGatewayBindingPort gatewayBindingPort = new FakeLoadGatewayBindingPort(Optional.of(
                WorkspaceGatewayBinding.create(101L, "https://gateway.example.com", "secret-token", "admin-1")));
        FakeSyncGatewayPort syncGatewayPort = new FakeSyncGatewayPort(new SyncWorkspaceAgentSkillFilesResult(
                false,
                "gateway should not be called",
                List.of()));

        UpdateWorkspaceAgentService service = new UpdateWorkspaceAgentService(
                memberPort,
                workspacePort,
                agentStore,
                agentStore,
                gatewayBindingPort,
                syncGatewayPort);

        UpdateWorkspaceAgentResult result = service.updateWorkspaceAgent(new UpdateWorkspaceAgentCommand(
                101L,
                501L,
                "admin-1",
                "Direct Agent",
                WorkspaceAgentCategory.QA,
                ".aquila/workspaces/direct",
                List.of(new WorkspaceAgentSkillFileCommand("AGENTS.md", "# rules"))));

        assertThat(syncGatewayPort.called).isFalse();
        assertThat(result.status()).isEqualTo(WorkspaceAgentStatus.READY);
        assertThat(result.skillFiles()).singleElement().satisfies(file -> {
            assertThat(file.fileName()).isEqualTo("AGENTS.md");
            assertThat(file.syncStatus()).isEqualTo(WorkspaceAgentSkillFileSyncStatus.SYNCED);
        });
    }

    private static GetWorkspaceDetailResult workspaceDetail(WorkspaceMemberRole role) {
        return new GetWorkspaceDetailResult(
                101L,
                "Team",
                "Desc",
                "admin-1",
                role,
                LocalDateTime.of(2026, 5, 16, 10, 0),
                null);
    }

    private static SavedWorkspaceAgent existingAgent(String openClawAgentId, WorkspaceAgentStatus status) {
        return new SavedWorkspaceAgent(
                501L,
                101L,
                "Old Agent",
                WorkspaceAgentCategory.BACKEND,
                openClawAgentId,
                ".aquila/workspaces/old",
                status,
                null,
                "admin-1",
                LocalDateTime.of(2026, 5, 16, 10, 0),
                LocalDateTime.of(2026, 5, 16, 10, 0),
                List.of(new SavedWorkspaceAgentSkillFile(
                        601L, "AGENTS.md", "old", WorkspaceAgentSkillFileSyncStatus.PENDING, null)));
    }

    private record FakeLoadMemberPort(Optional<Member> member) implements LoadMemberPort {
        @Override
        public Optional<Member> findByGoogleSub(String googleSub) {
            return member;
        }
    }

    private record FakeLoadWorkspacePort(Optional<GetWorkspaceDetailResult> detail) implements LoadWorkspacePort {
        @Override
        public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            return List.of();
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId, String googleSub) {
            return detail;
        }
    }

    private static final class FakeAgentStore implements LoadWorkspaceAgentPort, UpdateWorkspaceAgentPort {
        private final SavedWorkspaceAgent existing;
        private boolean updateCalled;

        private FakeAgentStore(SavedWorkspaceAgent existing) {
            this.existing = existing;
        }

        @Override
        public List<SavedWorkspaceAgent> findActiveByWorkspaceId(Long workspaceId) {
            return List.of(existing);
        }

        @Override
        public Optional<SavedWorkspaceAgent> findActiveByWorkspaceIdAndAgentId(Long workspaceId, Long agentId) {
            return Optional.of(existing);
        }

        @Override
        public SavedWorkspaceAgent update(WorkspaceAgent agent, List<WorkspaceAgentSkillFile> replacedSkillFiles) {
            this.updateCalled = true;
            List<SavedWorkspaceAgentSkillFile> files = replacedSkillFiles == null
                    ? existing.skillFiles()
                    : replacedSkillFiles.stream()
                            .map(file -> new SavedWorkspaceAgentSkillFile(
                                    701L,
                                    file.getFileName(),
                                    file.getContent(),
                                    file.getSyncStatus(),
                                    file.getSyncError()))
                            .toList();
            return new SavedWorkspaceAgent(
                    agent.getId(),
                    agent.getWorkspaceId(),
                    agent.getName(),
                    agent.getCategory(),
                    agent.getOpenClawAgentId(),
                    agent.getWorkspacePath(),
                    agent.getStatus(),
                    agent.getSyncError(),
                    agent.getCreatedByGoogleSub(),
                    existing.createdAt(),
                    LocalDateTime.of(2026, 5, 16, 10, 5),
                    files,
                    agent.getModelProviderId(),
                    agent.getModelId(),
                    agent.getFallbackModelId());
        }
    }

    private record FakeLoadGatewayBindingPort(Optional<WorkspaceGatewayBinding> binding)
            implements LoadWorkspaceGatewayBindingPort {
        @Override
        public Optional<WorkspaceGatewayBinding> findActiveByWorkspaceId(Long workspaceId) {
            return binding;
        }
    }

    private static final class FakeSyncGatewayPort implements SyncWorkspaceAgentSkillFilesGatewayPort {
        private final SyncWorkspaceAgentSkillFilesResult result;
        private boolean called;
        private SyncWorkspaceAgentSkillFilesCommand lastCommand;

        private FakeSyncGatewayPort(SyncWorkspaceAgentSkillFilesResult result) {
            this.result = result;
        }

        @Override
        public SyncWorkspaceAgentSkillFilesResult syncSkillFiles(SyncWorkspaceAgentSkillFilesCommand command) {
            this.called = true;
            this.lastCommand = command;
            return result;
        }
    }
}
