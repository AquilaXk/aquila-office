package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CreateWorkspaceAgentCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.out.CreateWorkspaceAgentGatewayCommand;
import com.aquilaoffice.application.port.out.CreateWorkspaceAgentGatewayPort;
import com.aquilaoffice.application.port.out.CreateWorkspaceAgentGatewayResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.LoadWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceAgentPort;
import com.aquilaoffice.domain.agent.WorkspaceAgent;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFile;
import com.aquilaoffice.domain.agent.WorkspaceAgentTemplate;
import com.aquilaoffice.domain.agent.WorkspaceAgentTemplateCatalog;
import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 워크스페이스 관리자만 Agent를 생성할 수 있도록 멤버십을 검증한다.
 */
public class CreateWorkspaceAgentService implements CreateWorkspaceAgentUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final SaveWorkspaceAgentPort saveWorkspaceAgentPort;
    private final LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort;
    private final CreateWorkspaceAgentGatewayPort createWorkspaceAgentGatewayPort;
    private final WorkspaceAgentTemplateCatalog templateCatalog;

    public CreateWorkspaceAgentService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            SaveWorkspaceAgentPort saveWorkspaceAgentPort,
            LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort,
            CreateWorkspaceAgentGatewayPort createWorkspaceAgentGatewayPort) {
        this.loadMemberPort = loadMemberPort;
        this.loadWorkspacePort = loadWorkspacePort;
        this.saveWorkspaceAgentPort = saveWorkspaceAgentPort;
        this.loadWorkspaceGatewayBindingPort = loadWorkspaceGatewayBindingPort;
        this.createWorkspaceAgentGatewayPort = createWorkspaceAgentGatewayPort;
        this.templateCatalog = new WorkspaceAgentTemplateCatalog();
    }

    @Override
    public CreateWorkspaceAgentResult createWorkspaceAgent(CreateWorkspaceAgentCommand command) {
        requireExistingMember(command.requesterGoogleSub());
        requireAdminMembership(command.workspaceId(), command.requesterGoogleSub());

        WorkspaceAgent agent = WorkspaceAgent.create(
                command.workspaceId(),
                command.name(),
                command.category(),
                command.workspacePath(),
                command.requesterGoogleSub());
        List<WorkspaceAgentSkillFile> skillFiles = mergeSkillFiles(command);
        WorkspaceGatewayBinding gatewayBinding = loadGatewayBinding(command.workspaceId());
        CreateWorkspaceAgentGatewayResult gatewayResult =
                createWorkspaceAgentGatewayPort.create(
                        toGatewayCreateCommand(command, agent, skillFiles, gatewayBinding));
        applyGatewayCreateResult(agent, gatewayResult);
        applyGatewaySkillFileResults(skillFiles, gatewayResult);

        return WorkspaceAgentResultMapper.toCreateResult(saveWorkspaceAgentPort.save(agent, skillFiles));
    }

    private List<WorkspaceAgentSkillFile> mergeSkillFiles(CreateWorkspaceAgentCommand command) {
        Map<String, WorkspaceAgentSkillFile> mergedSkillFiles = new LinkedHashMap<>();
        for (WorkspaceAgentTemplate template : templateCatalog.defaultsFor(command.category())) {
            mergedSkillFiles.put(
                    template.fileName(),
                    WorkspaceAgentSkillFile.create(template.fileName(), template.content()));
        }
        for (var requestedSkillFile : command.skillFiles()) {
            WorkspaceAgentSkillFile skillFile = WorkspaceAgentSkillFile.create(
                    requestedSkillFile.fileName(),
                    requestedSkillFile.content());
            mergedSkillFiles.put(skillFile.getFileName(), skillFile);
        }
        return List.copyOf(mergedSkillFiles.values());
    }

    private WorkspaceGatewayBinding loadGatewayBinding(Long workspaceId) {
        return loadWorkspaceGatewayBindingPort.findActiveByWorkspaceId(workspaceId)
                .orElseThrow(() -> new IllegalStateException("workspace gateway binding required"));
    }

    private CreateWorkspaceAgentGatewayCommand toGatewayCreateCommand(
            CreateWorkspaceAgentCommand command,
            WorkspaceAgent agent,
            List<WorkspaceAgentSkillFile> skillFiles,
            WorkspaceGatewayBinding gatewayBinding) {
        return new CreateWorkspaceAgentGatewayCommand(
                agent.getWorkspaceId(),
                gatewayBinding.getGatewayUrl(),
                gatewayBinding.getToken(),
                agent.getName(),
                command.emoji(),
                agent.getCategory(),
                agent.getWorkspacePath(),
                skillFiles.stream()
                        .map(skillFile -> new CreateWorkspaceAgentGatewayCommand.SkillFile(
                                skillFile.getFileName(),
                                skillFile.getContent()))
                        .toList());
    }

    private void applyGatewayCreateResult(
            WorkspaceAgent agent,
            CreateWorkspaceAgentGatewayResult gatewayResult) {
        if (gatewayResult != null && gatewayResult.openClawAgentId() != null
                && !gatewayResult.openClawAgentId().isBlank()) {
            agent.markReady(gatewayResult.openClawAgentId());
        }
        if (gatewayResult != null && gatewayResult.success() && agent.getOpenClawAgentId() != null) {
            return;
        }
        agent.markSyncFailed(resolveGatewayFailure(gatewayResult));
    }

    private void applyGatewaySkillFileResults(
            List<WorkspaceAgentSkillFile> skillFiles,
            CreateWorkspaceAgentGatewayResult gatewayResult) {
        if (gatewayResult == null || gatewayResult.skillFiles().isEmpty()) {
            if (gatewayResult != null && gatewayResult.success()) {
                skillFiles.forEach(WorkspaceAgentSkillFile::markSynced);
                return;
            }
            String failureReason = resolveGatewayFailure(gatewayResult);
            skillFiles.forEach(skillFile -> skillFile.markFailed(failureReason));
            return;
        }

        Map<String, CreateWorkspaceAgentGatewayResult.SkillFileResult> resultsByFileName =
                gatewayResult.skillFiles().stream()
                        .collect(Collectors.toMap(
                                CreateWorkspaceAgentGatewayResult.SkillFileResult::fileName,
                                Function.identity(),
                                (first, second) -> second,
                                LinkedHashMap::new));
        for (WorkspaceAgentSkillFile skillFile : skillFiles) {
            CreateWorkspaceAgentGatewayResult.SkillFileResult result = resultsByFileName.get(skillFile.getFileName());
            if (result != null && result.success()) {
                skillFile.markSynced();
                continue;
            }
            String failureReason = result == null ? resolveGatewayFailure(gatewayResult) : result.failureReason();
            skillFile.markFailed(resolveSkillFileFailure(failureReason));
        }
    }

    private String resolveGatewayFailure(CreateWorkspaceAgentGatewayResult gatewayResult) {
        if (gatewayResult == null || gatewayResult.failureReason() == null
                || gatewayResult.failureReason().isBlank()) {
            return "gateway agent create failed";
        }
        return gatewayResult.failureReason().trim();
    }

    private String resolveSkillFileFailure(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return "gateway skill file sync failed";
        }
        return failureReason.trim();
    }

    private void requireExistingMember(String googleSub) {
        loadMemberPort.findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
    }

    private void requireAdminMembership(Long workspaceId, String googleSub) {
        var workspace = loadWorkspacePort.findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
        if (workspace.myRole() != WorkspaceMemberRole.ADMIN) {
            throw new IllegalArgumentException("workspace admin membership required");
        }
    }
}
