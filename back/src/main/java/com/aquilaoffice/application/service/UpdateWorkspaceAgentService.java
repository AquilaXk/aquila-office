package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.UpdateWorkspaceAgentCommand;
import com.aquilaoffice.application.port.in.UpdateWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.UpdateWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.in.WorkspaceAgentSkillFileCommand;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgent;
import com.aquilaoffice.application.port.out.SyncWorkspaceAgentSkillFilesCommand;
import com.aquilaoffice.application.port.out.SyncWorkspaceAgentSkillFilesGatewayPort;
import com.aquilaoffice.application.port.out.SyncWorkspaceAgentSkillFilesResult;
import com.aquilaoffice.application.port.out.UpdateWorkspaceAgentPort;
import com.aquilaoffice.domain.agent.WorkspaceAgent;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFile;
import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 워크스페이스 관리자만 Agent 메타데이터와 skill 파일을 수정하고 Gateway에 동기화할 수 있게 처리한다.
 */
public class UpdateWorkspaceAgentService implements UpdateWorkspaceAgentUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceAgentPort loadWorkspaceAgentPort;
    private final UpdateWorkspaceAgentPort updateWorkspaceAgentPort;
    private final LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort;
    private final SyncWorkspaceAgentSkillFilesGatewayPort syncWorkspaceAgentSkillFilesGatewayPort;

    public UpdateWorkspaceAgentService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort,
            UpdateWorkspaceAgentPort updateWorkspaceAgentPort,
            LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort,
            SyncWorkspaceAgentSkillFilesGatewayPort syncWorkspaceAgentSkillFilesGatewayPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadWorkspaceAgentPort =
                Objects.requireNonNull(loadWorkspaceAgentPort, "loadWorkspaceAgentPort must not be null");
        this.updateWorkspaceAgentPort =
                Objects.requireNonNull(updateWorkspaceAgentPort, "updateWorkspaceAgentPort must not be null");
        this.loadWorkspaceGatewayBindingPort = Objects.requireNonNull(
                loadWorkspaceGatewayBindingPort, "loadWorkspaceGatewayBindingPort must not be null");
        this.syncWorkspaceAgentSkillFilesGatewayPort = Objects.requireNonNull(
                syncWorkspaceAgentSkillFilesGatewayPort,
                "syncWorkspaceAgentSkillFilesGatewayPort must not be null");
    }

    @Override
    public UpdateWorkspaceAgentResult updateWorkspaceAgent(UpdateWorkspaceAgentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        requireExistingMember(command.requesterGoogleSub());
        requireAdminMembership(command.workspaceId(), command.requesterGoogleSub());

        SavedWorkspaceAgent existing = loadWorkspaceAgentPort
                .findActiveByWorkspaceIdAndAgentId(command.workspaceId(), command.agentId())
                .orElseThrow(() -> new IllegalArgumentException("workspace agent not found"));

        WorkspaceAgent agent = WorkspaceAgent.restore(
                existing.agentId(),
                existing.workspaceId(),
                existing.name(),
                existing.category(),
                existing.openClawAgentId(),
                existing.workspacePath(),
                existing.status(),
                existing.syncError(),
                existing.createdByGoogleSub());
        agent.update(command.name(), command.category(), command.workspacePath());

        List<WorkspaceAgentSkillFile> replacedSkillFiles = toReplacedSkillFiles(command.skillFiles());
        if (replacedSkillFiles != null) {
            syncSkillFilesIfGatewayAvailable(agent, replacedSkillFiles);
        }

        return WorkspaceAgentResultMapper.toUpdateResult(
                updateWorkspaceAgentPort.update(agent, replacedSkillFiles));
    }

    private List<WorkspaceAgentSkillFile> toReplacedSkillFiles(List<WorkspaceAgentSkillFileCommand> skillFiles) {
        if (skillFiles == null) {
            return null;
        }
        Map<String, WorkspaceAgentSkillFile> deduplicated = new LinkedHashMap<>();
        for (WorkspaceAgentSkillFileCommand item : skillFiles) {
            WorkspaceAgentSkillFile skillFile = WorkspaceAgentSkillFile.create(item.fileName(), item.content());
            deduplicated.put(skillFile.getFileName(), skillFile);
        }
        return List.copyOf(deduplicated.values());
    }

    private void syncSkillFilesIfGatewayAvailable(
            WorkspaceAgent agent,
            List<WorkspaceAgentSkillFile> skillFiles) {
        if (agent.getOpenClawAgentId() == null || agent.getOpenClawAgentId().isBlank()) {
            return;
        }
        Optional<WorkspaceGatewayBinding> bindingOptional =
                loadWorkspaceGatewayBindingPort.findActiveByWorkspaceId(agent.getWorkspaceId());
        if (bindingOptional.isEmpty()) {
            return;
        }
        if (skillFiles.isEmpty()) {
            agent.markReady(agent.getOpenClawAgentId());
            return;
        }

        WorkspaceGatewayBinding binding = bindingOptional.get();
        SyncWorkspaceAgentSkillFilesResult syncResult = syncWorkspaceAgentSkillFilesGatewayPort.syncSkillFiles(
                new SyncWorkspaceAgentSkillFilesCommand(
                        agent.getWorkspaceId(),
                        binding.getGatewayUrl(),
                        binding.getToken(),
                        agent.getOpenClawAgentId(),
                        skillFiles.stream()
                                .map(file -> new SyncWorkspaceAgentSkillFilesCommand.SkillFile(
                                        file.getFileName(),
                                        file.getContent()))
                                .toList()));

        applySyncResults(agent, skillFiles, syncResult);
    }

    private void applySyncResults(
            WorkspaceAgent agent,
            List<WorkspaceAgentSkillFile> skillFiles,
            SyncWorkspaceAgentSkillFilesResult syncResult) {
        if (syncResult == null) {
            agent.markSyncFailed("gateway skill file sync failed");
            skillFiles.forEach(file -> file.markFailed("gateway skill file sync failed"));
            return;
        }
        if (syncResult.skillFiles().isEmpty()) {
            if (syncResult.success()) {
                skillFiles.forEach(WorkspaceAgentSkillFile::markSynced);
                agent.markReady(agent.getOpenClawAgentId());
                return;
            }
            String failureReason = resolveFailureReason(syncResult.failureReason());
            skillFiles.forEach(file -> file.markFailed(failureReason));
            agent.markSyncFailed(failureReason);
            return;
        }

        Map<String, SyncWorkspaceAgentSkillFilesResult.SkillFileResult> byFileName = syncResult.skillFiles().stream()
                .collect(Collectors.toMap(
                        SyncWorkspaceAgentSkillFilesResult.SkillFileResult::fileName,
                        Function.identity(),
                        (first, second) -> second,
                        LinkedHashMap::new));
        boolean allSynced = true;
        for (WorkspaceAgentSkillFile file : skillFiles) {
            SyncWorkspaceAgentSkillFilesResult.SkillFileResult fileResult = byFileName.get(file.getFileName());
            if (fileResult != null && fileResult.success()) {
                file.markSynced();
            } else {
                allSynced = false;
                String reason = fileResult == null
                        ? resolveFailureReason(syncResult.failureReason())
                        : resolveFailureReason(fileResult.failureReason());
                file.markFailed(reason);
            }
        }
        if (allSynced && syncResult.success()) {
            agent.markReady(agent.getOpenClawAgentId());
        } else {
            agent.markSyncFailed(resolveFailureReason(syncResult.failureReason()));
        }
    }

    private static String resolveFailureReason(String failureReason) {
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
