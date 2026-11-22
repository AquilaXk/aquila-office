package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CreateWorkspaceModelProviderCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceModelProviderUseCase;
import com.aquilaoffice.application.port.in.DeleteWorkspaceModelProviderUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceModelProvidersUseCase;
import com.aquilaoffice.application.port.in.ListWorkspaceModelTelemetryUseCase;
import com.aquilaoffice.application.port.in.TestWorkspaceModelProviderCommand;
import com.aquilaoffice.application.port.in.TestWorkspaceModelProviderUseCase;
import com.aquilaoffice.application.port.in.UpdateWorkspaceModelProviderCommand;
import com.aquilaoffice.application.port.in.UpdateWorkspaceModelProviderUseCase;
import com.aquilaoffice.application.port.in.WorkspaceModelConnectionTestResult;
import com.aquilaoffice.application.port.in.WorkspaceModelProviderResult;
import com.aquilaoffice.application.port.out.LoadModelCatalogPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionModelTelemetryPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceModelProviderPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.RedactSensitiveTextPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceModelProviderPort;
import com.aquilaoffice.application.port.out.TestWorkspaceModelProviderPort;
import com.aquilaoffice.application.port.out.WorkspaceModelConnectionTestRequest;
import com.aquilaoffice.application.port.out.WorkspaceModelTelemetryEntry;
import com.aquilaoffice.domain.model.ModelCatalogPreset;
import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelWireProtocol;
import com.aquilaoffice.domain.model.WorkspaceModelProvider;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/**
 * 워크스페이스 AI 모델 공급자 등록/조회/수정/삭제 및 연결 진단을 조율한다.
 */
public class WorkspaceModelProviderService
        implements ListWorkspaceModelProvidersUseCase,
                ListWorkspaceModelTelemetryUseCase,
                CreateWorkspaceModelProviderUseCase,
                UpdateWorkspaceModelProviderUseCase,
                DeleteWorkspaceModelProviderUseCase,
                TestWorkspaceModelProviderUseCase {

    private static final int DEFAULT_TELEMETRY_LIMIT = 20;

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceModelProviderPort loadWorkspaceModelProviderPort;
    private final SaveWorkspaceModelProviderPort saveWorkspaceModelProviderPort;
    private final TestWorkspaceModelProviderPort testWorkspaceModelProviderPort;
    private final LoadModelCatalogPort loadModelCatalogPort;
    private final RedactSensitiveTextPort redactSensitiveTextPort;
    private final LoadTaskExecutionModelTelemetryPort loadTaskExecutionModelTelemetryPort;
    private final Clock clock;

    public WorkspaceModelProviderService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceModelProviderPort loadWorkspaceModelProviderPort,
            SaveWorkspaceModelProviderPort saveWorkspaceModelProviderPort,
            TestWorkspaceModelProviderPort testWorkspaceModelProviderPort,
            LoadModelCatalogPort loadModelCatalogPort,
            RedactSensitiveTextPort redactSensitiveTextPort,
            Clock clock) {
        this(
                loadMemberPort,
                loadWorkspacePort,
                loadWorkspaceModelProviderPort,
                saveWorkspaceModelProviderPort,
                testWorkspaceModelProviderPort,
                loadModelCatalogPort,
                redactSensitiveTextPort,
                (workspaceId, limit) -> List.of(),
                clock);
    }

    public WorkspaceModelProviderService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceModelProviderPort loadWorkspaceModelProviderPort,
            SaveWorkspaceModelProviderPort saveWorkspaceModelProviderPort,
            TestWorkspaceModelProviderPort testWorkspaceModelProviderPort,
            LoadModelCatalogPort loadModelCatalogPort,
            RedactSensitiveTextPort redactSensitiveTextPort,
            LoadTaskExecutionModelTelemetryPort loadTaskExecutionModelTelemetryPort,
            Clock clock) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadWorkspaceModelProviderPort = Objects.requireNonNull(
                loadWorkspaceModelProviderPort, "loadWorkspaceModelProviderPort must not be null");
        this.saveWorkspaceModelProviderPort = Objects.requireNonNull(
                saveWorkspaceModelProviderPort, "saveWorkspaceModelProviderPort must not be null");
        this.testWorkspaceModelProviderPort = Objects.requireNonNull(
                testWorkspaceModelProviderPort, "testWorkspaceModelProviderPort must not be null");
        this.loadModelCatalogPort = Objects.requireNonNull(
                loadModelCatalogPort, "loadModelCatalogPort must not be null");
        this.redactSensitiveTextPort = Objects.requireNonNull(
                redactSensitiveTextPort, "redactSensitiveTextPort must not be null");
        this.loadTaskExecutionModelTelemetryPort = Objects.requireNonNull(
                loadTaskExecutionModelTelemetryPort, "loadTaskExecutionModelTelemetryPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public List<WorkspaceModelProviderResult> list(Long workspaceId, String requesterGoogleSub) {
        requireExistingMember(requesterGoogleSub);
        requireJoinedMembership(workspaceId, requesterGoogleSub);
        return loadWorkspaceModelProviderPort.findActiveByWorkspaceId(workspaceId).stream()
                .map(WorkspaceModelProviderResult::from)
                .toList();
    }

    @Override
    public List<WorkspaceModelTelemetryEntry> listTelemetry(Long workspaceId, String requesterGoogleSub) {
        requireExistingMember(requesterGoogleSub);
        requireJoinedMembership(workspaceId, requesterGoogleSub);
        return loadTaskExecutionModelTelemetryPort.loadModelTelemetry(workspaceId, DEFAULT_TELEMETRY_LIMIT);
    }

    @Override
    public WorkspaceModelProviderResult create(CreateWorkspaceModelProviderCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        requireExistingMember(command.requesterGoogleSub());
        requireAdminMembership(command.workspaceId(), command.requesterGoogleSub());

        String resolvedBaseUrl = resolveBaseUrl(command.providerKind(), command.baseUrl());
        ModelWireProtocol resolvedProtocol = command.wireProtocol() != null
                ? command.wireProtocol()
                : defaultWireProtocol(command.providerKind());
        WorkspaceModelProvider provider = WorkspaceModelProvider.create(
                command.workspaceId(),
                command.providerKind(),
                resolvedProtocol,
                command.displayName(),
                resolvedBaseUrl,
                command.apiKey(),
                command.defaultModel(),
                command.enabledModels(),
                command.priorityOrder(),
                command.enabled(),
                command.requesterGoogleSub());
        return WorkspaceModelProviderResult.from(saveWorkspaceModelProviderPort.save(provider));
    }

    @Override
    public WorkspaceModelProviderResult update(UpdateWorkspaceModelProviderCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        requireExistingMember(command.requesterGoogleSub());
        requireAdminMembership(command.workspaceId(), command.requesterGoogleSub());

        WorkspaceModelProvider provider = loadWorkspaceModelProviderPort
                .findActiveByWorkspaceIdAndProviderId(command.workspaceId(), command.providerId())
                .orElseThrow(() -> new IllegalArgumentException("workspace model provider not found"));
        provider.update(
                command.displayName(),
                command.wireProtocol(),
                command.baseUrl(),
                command.apiKey(),
                command.defaultModel(),
                command.enabledModels(),
                command.priorityOrder(),
                command.enabled());
        return WorkspaceModelProviderResult.from(saveWorkspaceModelProviderPort.save(provider));
    }

    @Override
    public void delete(Long workspaceId, Long providerId, String requesterGoogleSub) {
        requireExistingMember(requesterGoogleSub);
        requireAdminMembership(workspaceId, requesterGoogleSub);

        WorkspaceModelProvider provider = loadWorkspaceModelProviderPort
                .findActiveByWorkspaceIdAndProviderId(workspaceId, providerId)
                .orElseThrow(() -> new IllegalArgumentException("workspace model provider not found"));
        provider.markDeleted(LocalDateTime.now(clock.withZone(ZoneOffset.UTC)));
        saveWorkspaceModelProviderPort.save(provider);
    }

    @Override
    public WorkspaceModelConnectionTestResult testConnection(TestWorkspaceModelProviderCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        requireExistingMember(command.requesterGoogleSub());
        requireAdminMembership(command.workspaceId(), command.requesterGoogleSub());

        WorkspaceModelProvider existing = null;
        if (command.providerId() != null) {
            existing = loadWorkspaceModelProviderPort
                    .findActiveByWorkspaceIdAndProviderId(command.workspaceId(), command.providerId())
                    .orElseThrow(() -> new IllegalArgumentException("workspace model provider not found"));
        }

        ModelProviderKind providerKind = command.providerKind() != null
                ? command.providerKind()
                : (existing != null ? existing.getProviderKind() : null);
        if (providerKind == null) {
            throw new IllegalArgumentException("providerKind must not be null");
        }
        ModelWireProtocol wireProtocol = command.wireProtocol() != null
                ? command.wireProtocol()
                : (existing != null ? existing.getWireProtocol() : defaultWireProtocol(providerKind));
        String baseUrl = firstNonBlank(
                command.baseUrl(),
                existing != null ? existing.getBaseUrl() : resolveBaseUrl(providerKind, null));
        String apiKey = firstNonBlank(
                command.apiKey(),
                existing != null ? existing.getApiKey() : null);
        String modelId = firstNonBlank(
                command.modelId(),
                existing != null ? existing.getDefaultModel() : defaultPresetModel(providerKind));
        String mode = "completion".equalsIgnoreCase(firstNonBlank(command.mode(), "models"))
                ? "completion"
                : "models";

        WorkspaceModelConnectionTestResult rawResult = testWorkspaceModelProviderPort.testConnection(
                new WorkspaceModelConnectionTestRequest(
                        providerKind,
                        wireProtocol,
                        baseUrl,
                        apiKey,
                        modelId,
                        mode));
        String safeMessage = redactSensitiveTextPort.redact(rawResult.message());
        WorkspaceModelConnectionTestResult sanitizedResult = new WorkspaceModelConnectionTestResult(
                rawResult.ok(),
                rawResult.status(),
                rawResult.latencyMs(),
                rawResult.discoveredModels(),
                rawResult.resolvedModel(),
                rawResult.replyPreview(),
                rawResult.promptTokens(),
                rawResult.completionTokens(),
                safeMessage);

        if (existing != null) {
            existing.recordConnectionCheck(
                    sanitizedResult.status(),
                    sanitizedResult.latencyMs(),
                    LocalDateTime.now(clock.withZone(ZoneOffset.UTC)),
                    sanitizedResult.ok() ? null : safeMessage);
            saveWorkspaceModelProviderPort.save(existing);
        }
        return sanitizedResult;
    }

    private String resolveBaseUrl(ModelProviderKind providerKind, String requestedBaseUrl) {
        if (requestedBaseUrl != null && !requestedBaseUrl.isBlank()) {
            return requestedBaseUrl.trim();
        }
        return loadModelCatalogPort.loadPresets().stream()
                .filter(preset -> preset.providerKind() == providerKind)
                .map(ModelCatalogPreset::defaultBaseUrl)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("baseUrl is required for provider " + providerKind));
    }

    private ModelWireProtocol defaultWireProtocol(ModelProviderKind providerKind) {
        if (providerKind == null) {
            throw new IllegalArgumentException("providerKind must not be null");
        }
        return loadModelCatalogPort.loadPresets().stream()
                .filter(preset -> preset.providerKind() == providerKind)
                .map(ModelCatalogPreset::wireProtocol)
                .findFirst()
                .orElse(providerKind.defaultWireProtocol());
    }

    private String defaultPresetModel(ModelProviderKind providerKind) {
        return loadModelCatalogPort.loadPresets().stream()
                .filter(preset -> preset.providerKind() == providerKind)
                .flatMap(preset -> preset.models().stream())
                .map(model -> model.id())
                .findFirst()
                .orElse(null);
    }

    private void requireExistingMember(String googleSub) {
        loadMemberPort.findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
    }

    private void requireJoinedMembership(Long workspaceId, String googleSub) {
        loadWorkspacePort.findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
    }

    private void requireAdminMembership(Long workspaceId, String googleSub) {
        var workspace = loadWorkspacePort.findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
        if (workspace.myRole() != WorkspaceMemberRole.ADMIN) {
            throw new IllegalArgumentException("workspace admin membership required");
        }
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }
}
