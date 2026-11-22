package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CreateWorkspaceAgentCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceAgentResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceAgentUseCase;
import com.aquilaoffice.application.port.out.CreateWorkspaceAgentGatewayCommand;
import com.aquilaoffice.application.port.out.CreateWorkspaceAgentGatewayPort;
import com.aquilaoffice.application.port.out.CreateWorkspaceAgentGatewayResult;
import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentGatewayCommand;
import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentGatewayPort;
import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentGatewayResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceModelProviderPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveWorkspaceAgentPort;
import com.aquilaoffice.domain.agent.WorkspaceAgent;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFile;
import com.aquilaoffice.domain.agent.WorkspaceAgentTemplate;
import com.aquilaoffice.domain.agent.WorkspaceAgentTemplateCatalog;
import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;
import com.aquilaoffice.domain.model.WorkspaceModelProvider;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 워크스페이스 관리자만 Agent를 생성할 수 있도록 검증하고, OpenClaw Gateway 또는 직접 모델 공급자 모드로 활성화한다.
 */
public class CreateWorkspaceAgentService implements CreateWorkspaceAgentUseCase {

    private static final String DIRECT_MODEL_PREFIX = "direct-model:";

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final SaveWorkspaceAgentPort saveWorkspaceAgentPort;
    private final LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort;
    private final CreateWorkspaceAgentGatewayPort createWorkspaceAgentGatewayPort;
    private final DeleteWorkspaceAgentGatewayPort deleteWorkspaceAgentGatewayPort;
    private final LoadWorkspaceModelProviderPort loadWorkspaceModelProviderPort;
    private final WorkspaceAgentTemplateCatalog templateCatalog;

    public CreateWorkspaceAgentService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            SaveWorkspaceAgentPort saveWorkspaceAgentPort,
            LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort,
            CreateWorkspaceAgentGatewayPort createWorkspaceAgentGatewayPort) {
        this(
                loadMemberPort,
                loadWorkspacePort,
                saveWorkspaceAgentPort,
                loadWorkspaceGatewayBindingPort,
                createWorkspaceAgentGatewayPort,
                command -> new DeleteWorkspaceAgentGatewayResult(true, null),
                new LoadWorkspaceModelProviderPort() {
                    @Override
                    public List<WorkspaceModelProvider> findActiveByWorkspaceId(Long workspaceId) {
                        return List.of();
                    }

                    @Override
                    public Optional<WorkspaceModelProvider> findActiveByWorkspaceIdAndProviderId(
                            Long workspaceId,
                            Long providerId) {
                        return Optional.empty();
                    }
                });
    }

    public CreateWorkspaceAgentService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            SaveWorkspaceAgentPort saveWorkspaceAgentPort,
            LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort,
            CreateWorkspaceAgentGatewayPort createWorkspaceAgentGatewayPort,
            DeleteWorkspaceAgentGatewayPort deleteWorkspaceAgentGatewayPort,
            LoadWorkspaceModelProviderPort loadWorkspaceModelProviderPort) {
        this.loadMemberPort = loadMemberPort;
        this.loadWorkspacePort = loadWorkspacePort;
        this.saveWorkspaceAgentPort = saveWorkspaceAgentPort;
        this.loadWorkspaceGatewayBindingPort = loadWorkspaceGatewayBindingPort;
        this.createWorkspaceAgentGatewayPort = createWorkspaceAgentGatewayPort;
        this.deleteWorkspaceAgentGatewayPort = deleteWorkspaceAgentGatewayPort;
        this.loadWorkspaceModelProviderPort = loadWorkspaceModelProviderPort;
        this.templateCatalog = new WorkspaceAgentTemplateCatalog();
    }

    @Override
    public CreateWorkspaceAgentResult createWorkspaceAgent(CreateWorkspaceAgentCommand command) {
        requireExistingMember(command.requesterGoogleSub());
        requireAdminMembership(command.workspaceId(), command.requesterGoogleSub());
        validateRequestedModelProvider(command.workspaceId(), command.modelProviderId());

        WorkspaceAgent agent = WorkspaceAgent.create(
                command.workspaceId(),
                command.name(),
                command.category(),
                command.workspacePath(),
                command.requesterGoogleSub(),
                command.modelProviderId(),
                command.modelId(),
                command.fallbackModelId());
        List<WorkspaceAgentSkillFile> skillFiles = mergeSkillFiles(command);

        if (shouldUseDirectModelMode(command)) {
            agent.markReady(DIRECT_MODEL_PREFIX + UUID.randomUUID());
            skillFiles.forEach(WorkspaceAgentSkillFile::markSynced);
            return WorkspaceAgentResultMapper.toCreateResult(saveWorkspaceAgentPort.save(agent, skillFiles));
        }

        WorkspaceGatewayBinding gatewayBinding = loadGatewayBinding(command.workspaceId());
        CreateWorkspaceAgentGatewayResult gatewayResult = createWorkspaceAgentGatewayPort.create(
                toGatewayCreateCommand(command, agent, skillFiles, gatewayBinding));
        applyGatewayCreateResult(agent, gatewayResult);
        applyGatewaySkillFileResults(skillFiles, gatewayResult);

        try {
            return WorkspaceAgentResultMapper.toCreateResult(saveWorkspaceAgentPort.save(agent, skillFiles));
        } catch (RuntimeException dbException) {
            compensateGatewayAgentCreate(gatewayBinding, agent.getWorkspaceId(), agent.getOpenClawAgentId());
            throw dbException;
        }
    }

    private void validateRequestedModelProvider(Long workspaceId, Long modelProviderId) {
        if (modelProviderId == null) {
            return;
        }
        WorkspaceModelProvider provider = loadWorkspaceModelProviderPort
                .findActiveByWorkspaceIdAndProviderId(workspaceId, modelProviderId)
                .orElseThrow(() -> new IllegalArgumentException("workspace model provider not found"));
        if (!provider.isEnabled()) {
            throw new IllegalArgumentException("workspace model provider is disabled");
        }
    }

    private boolean shouldUseDirectModelMode(CreateWorkspaceAgentCommand command) {
        if (command.modelProviderId() != null) {
            return true;
        }
        Optional<WorkspaceGatewayBinding> binding =
                loadWorkspaceGatewayBindingPort.findActiveByWorkspaceId(command.workspaceId());
        if (binding.isPresent()) {
            return false;
        }
        return loadWorkspaceModelProviderPort.findActiveByWorkspaceId(command.workspaceId()).stream()
                .anyMatch(WorkspaceModelProvider::isEnabled);
    }

    private void compensateGatewayAgentCreate(
            WorkspaceGatewayBinding gatewayBinding,
            Long workspaceId,
            String openClawAgentId) {
        if (openClawAgentId == null || openClawAgentId.isBlank()
                || openClawAgentId.startsWith(DIRECT_MODEL_PREFIX)) {
            return;
        }
        try {
            deleteWorkspaceAgentGatewayPort.delete(new DeleteWorkspaceAgentGatewayCommand(
                    workspaceId,
                    gatewayBinding.getGatewayUrl(),
                    gatewayBinding.getToken(),
                    openClawAgentId));
        } catch (RuntimeException ignored) {
            // DB 저장 실패 시 원래 예외를 보존하기 위해 보상 삭제 실패는 삼킨다.
        }
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
