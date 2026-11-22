package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.RunTaskCommand;
import com.aquilaoffice.application.port.in.RunTaskResult;
import com.aquilaoffice.application.port.in.RunTaskUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionTokenUsagePort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceModelProviderPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.RedactSensitiveTextPort;
import com.aquilaoffice.application.port.out.RunWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.RunWorkspaceModelPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionLogPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionPort;
import com.aquilaoffice.application.port.out.SavedTaskExecution;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgent;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgentSkillFile;
import com.aquilaoffice.application.port.out.TaskDetail;
import com.aquilaoffice.application.port.out.TaskExecutionLogCommand;
import com.aquilaoffice.application.port.out.TaskExecutionStatusUpdate;
import com.aquilaoffice.application.port.out.TaskStatusUpdate;
import com.aquilaoffice.application.port.out.UpdateTaskExecutionPort;
import com.aquilaoffice.application.port.out.UpdateTaskStatusPort;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunCommand;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunResult;
import com.aquilaoffice.application.port.out.WorkspaceExecutionLock;
import com.aquilaoffice.application.port.out.WorkspaceExecutionLockCommand;
import com.aquilaoffice.application.port.out.WorkspaceExecutionLockPort;
import com.aquilaoffice.application.port.out.WorkspaceModelRunCommand;
import com.aquilaoffice.application.port.out.WorkspaceModelRunResult;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.execution.TaskExecution;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.model.WorkspaceModelProvider;
import com.aquilaoffice.domain.task.TaskStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 기존 Task를 READY Agent(OpenClaw Gateway 또는 직접 AI 모델 공급자)로 실행하고 실행/작업/토큰 상태를 갱신한다.
 */
public class RunTaskService implements RunTaskUseCase {

    private static final String DIRECT_MODEL_PREFIX = "direct-model:";
    private static final String GATEWAY_BINDING_NOT_FOUND = "workspace gateway binding not found";
    private static final String GATEWAY_RUN_FAILED = "workspace agent run failed";
    private static final String MODEL_RUN_FAILED = "workspace model run failed";
    private static final String DEFAULT_MODEL_NAME = "gemini-2.5-flash";
    private static final int MAX_EXECUTION_TEXT_LENGTH = 1000;
    private static final int MAX_SYSTEM_PROMPT_LENGTH = 4000;
    private static final int CHARS_PER_TOKEN_ESTIMATE = 4;

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadTaskPort loadTaskPort;
    private final LoadWorkspaceAgentPort loadWorkspaceAgentPort;
    private final LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort;
    private final SaveTaskExecutionPort saveTaskExecutionPort;
    private final UpdateTaskExecutionPort updateTaskExecutionPort;
    private final UpdateTaskStatusPort updateTaskStatusPort;
    private final SaveTaskExecutionLogPort saveTaskExecutionLogPort;
    private final RunWorkspaceAgentPort runWorkspaceAgentPort;
    private final WorkspaceExecutionLockPort workspaceExecutionLockPort;
    private final WorkflowMetrics workflowMetrics;
    private final LoadTaskExecutionTokenUsagePort loadTaskExecutionTokenUsagePort;
    private final Clock clock;
    private final String defaultModelName;
    private final LoadWorkspaceModelProviderPort loadWorkspaceModelProviderPort;
    private final RunWorkspaceModelPort runWorkspaceModelPort;
    private final WorkspaceTokenBudgetGate workspaceTokenBudgetGate;
    private final RedactSensitiveTextPort redactSensitiveTextPort;

    public RunTaskService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort,
            LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort,
            SaveTaskExecutionPort saveTaskExecutionPort,
            UpdateTaskExecutionPort updateTaskExecutionPort,
            UpdateTaskStatusPort updateTaskStatusPort,
            SaveTaskExecutionLogPort saveTaskExecutionLogPort,
            RunWorkspaceAgentPort runWorkspaceAgentPort) {
        this(
                loadMemberPort,
                loadWorkspacePort,
                loadTaskPort,
                loadWorkspaceAgentPort,
                loadWorkspaceGatewayBindingPort,
                saveTaskExecutionPort,
                updateTaskExecutionPort,
                updateTaskStatusPort,
                saveTaskExecutionLogPort,
                runWorkspaceAgentPort,
                WorkspaceExecutionLockPort.alwaysAvailable(),
                WorkflowMetrics.noop());
    }

    public RunTaskService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort,
            LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort,
            SaveTaskExecutionPort saveTaskExecutionPort,
            UpdateTaskExecutionPort updateTaskExecutionPort,
            UpdateTaskStatusPort updateTaskStatusPort,
            SaveTaskExecutionLogPort saveTaskExecutionLogPort,
            RunWorkspaceAgentPort runWorkspaceAgentPort,
            WorkspaceExecutionLockPort workspaceExecutionLockPort) {
        this(
                loadMemberPort,
                loadWorkspacePort,
                loadTaskPort,
                loadWorkspaceAgentPort,
                loadWorkspaceGatewayBindingPort,
                saveTaskExecutionPort,
                updateTaskExecutionPort,
                updateTaskStatusPort,
                saveTaskExecutionLogPort,
                runWorkspaceAgentPort,
                workspaceExecutionLockPort,
                WorkflowMetrics.noop());
    }

    public RunTaskService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort,
            LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort,
            SaveTaskExecutionPort saveTaskExecutionPort,
            UpdateTaskExecutionPort updateTaskExecutionPort,
            UpdateTaskStatusPort updateTaskStatusPort,
            SaveTaskExecutionLogPort saveTaskExecutionLogPort,
            RunWorkspaceAgentPort runWorkspaceAgentPort,
            WorkspaceExecutionLockPort workspaceExecutionLockPort,
            WorkflowMetrics workflowMetrics) {
        this(
                loadMemberPort,
                loadWorkspacePort,
                loadTaskPort,
                loadWorkspaceAgentPort,
                loadWorkspaceGatewayBindingPort,
                saveTaskExecutionPort,
                updateTaskExecutionPort,
                updateTaskStatusPort,
                saveTaskExecutionLogPort,
                runWorkspaceAgentPort,
                workspaceExecutionLockPort,
                workflowMetrics,
                (workspaceId, sinceUtc) -> 0L,
                Clock.systemUTC(),
                DEFAULT_MODEL_NAME);
    }

    public RunTaskService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort,
            LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort,
            SaveTaskExecutionPort saveTaskExecutionPort,
            UpdateTaskExecutionPort updateTaskExecutionPort,
            UpdateTaskStatusPort updateTaskStatusPort,
            SaveTaskExecutionLogPort saveTaskExecutionLogPort,
            RunWorkspaceAgentPort runWorkspaceAgentPort,
            WorkspaceExecutionLockPort workspaceExecutionLockPort,
            WorkflowMetrics workflowMetrics,
            LoadTaskExecutionTokenUsagePort loadTaskExecutionTokenUsagePort,
            Clock clock,
            String defaultModelName) {
        this(
                loadMemberPort,
                loadWorkspacePort,
                loadTaskPort,
                loadWorkspaceAgentPort,
                loadWorkspaceGatewayBindingPort,
                saveTaskExecutionPort,
                updateTaskExecutionPort,
                updateTaskStatusPort,
                saveTaskExecutionLogPort,
                runWorkspaceAgentPort,
                workspaceExecutionLockPort,
                workflowMetrics,
                loadTaskExecutionTokenUsagePort,
                clock,
                defaultModelName,
                noopModelProviderPort(),
                cmd -> WorkspaceModelRunResult.fail(MODEL_RUN_FAILED, cmd.modelId(), false),
                new WorkspaceTokenBudgetGate(),
                text -> text);
    }

    public RunTaskService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort,
            LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort,
            SaveTaskExecutionPort saveTaskExecutionPort,
            UpdateTaskExecutionPort updateTaskExecutionPort,
            UpdateTaskStatusPort updateTaskStatusPort,
            SaveTaskExecutionLogPort saveTaskExecutionLogPort,
            RunWorkspaceAgentPort runWorkspaceAgentPort,
            WorkspaceExecutionLockPort workspaceExecutionLockPort,
            WorkflowMetrics workflowMetrics,
            LoadTaskExecutionTokenUsagePort loadTaskExecutionTokenUsagePort,
            Clock clock,
            String defaultModelName,
            LoadWorkspaceModelProviderPort loadWorkspaceModelProviderPort,
            RunWorkspaceModelPort runWorkspaceModelPort,
            WorkspaceTokenBudgetGate workspaceTokenBudgetGate,
            RedactSensitiveTextPort redactSensitiveTextPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "loadTaskPort must not be null");
        this.loadWorkspaceAgentPort =
                Objects.requireNonNull(loadWorkspaceAgentPort, "loadWorkspaceAgentPort must not be null");
        this.loadWorkspaceGatewayBindingPort = Objects.requireNonNull(
                loadWorkspaceGatewayBindingPort, "loadWorkspaceGatewayBindingPort must not be null");
        this.saveTaskExecutionPort =
                Objects.requireNonNull(saveTaskExecutionPort, "saveTaskExecutionPort must not be null");
        this.updateTaskExecutionPort =
                Objects.requireNonNull(updateTaskExecutionPort, "updateTaskExecutionPort must not be null");
        this.updateTaskStatusPort =
                Objects.requireNonNull(updateTaskStatusPort, "updateTaskStatusPort must not be null");
        this.saveTaskExecutionLogPort =
                Objects.requireNonNull(saveTaskExecutionLogPort, "saveTaskExecutionLogPort must not be null");
        this.runWorkspaceAgentPort =
                Objects.requireNonNull(runWorkspaceAgentPort, "runWorkspaceAgentPort must not be null");
        this.workspaceExecutionLockPort =
                Objects.requireNonNull(workspaceExecutionLockPort, "workspaceExecutionLockPort must not be null");
        this.workflowMetrics = Objects.requireNonNull(workflowMetrics, "workflowMetrics must not be null");
        this.loadTaskExecutionTokenUsagePort = Objects.requireNonNull(
                loadTaskExecutionTokenUsagePort, "loadTaskExecutionTokenUsagePort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.defaultModelName = normalizeModelName(defaultModelName);
        this.loadWorkspaceModelProviderPort = Objects.requireNonNull(
                loadWorkspaceModelProviderPort, "loadWorkspaceModelProviderPort must not be null");
        this.runWorkspaceModelPort =
                Objects.requireNonNull(runWorkspaceModelPort, "runWorkspaceModelPort must not be null");
        this.workspaceTokenBudgetGate =
                Objects.requireNonNull(workspaceTokenBudgetGate, "workspaceTokenBudgetGate must not be null");
        this.redactSensitiveTextPort =
                Objects.requireNonNull(redactSensitiveTextPort, "redactSensitiveTextPort must not be null");
    }

    @Override
    public RunTaskResult runTask(RunTaskCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String requesterGoogleSub = normalizeGoogleSub(command.requesterGoogleSub());
        Member requester = requireExistingMember(requesterGoogleSub);
        GetWorkspaceDetailResult workspace = requireJoinedWorkspace(command.workspaceId(), requester.getGoogleSub());
        TaskDetail task = loadTask(command.workspaceId(), command.taskId());
        requireRunnableTask(task);
        SavedWorkspaceAgent agent = resolveReadyAgent(task);
        WorkspaceExecutionLock lock = acquireWorkspaceLock(task, agent);
        WorkflowMetrics.OperationSample metricsSample = WorkflowMetrics.OperationSample.noop();
        try {
            metricsSample = workflowMetrics.startTaskExecution();
            WorkflowMetrics.OperationSample taskMetricsSample = metricsSample;
            SavedTaskExecution execution = saveTaskExecutionPort.save(
                    TaskExecution.create(command.workspaceId(), command.taskId(), agent.agentId()));
            saveExecutionLog(execution, "INFO", "Task execution queued");

            String prompt = resolvePrompt(task);
            int estimatedPromptTokens = estimateTokens(prompt);
            long todayUsedTokens = resolveTodayUsedTokens(workspace);
            try (WorkspaceTokenBudgetGate.BudgetReservation budgetReservation =
                    workspaceTokenBudgetGate.tryReserve(
                            workspace.workspaceId(),
                            workspace.dailyTokenLimit(),
                            todayUsedTokens,
                            estimatedPromptTokens)) {
                if (!budgetReservation.allowed()) {
                    return failTaskExecution(
                            task,
                            execution,
                            execution.agentId(),
                            budgetReservation.rejectionReason(),
                            taskMetricsSample);
                }

                Optional<WorkspaceGatewayBinding> gatewayBinding =
                        loadWorkspaceGatewayBindingPort.findActiveByWorkspaceId(command.workspaceId());
                if (shouldUseDirectModelExecution(agent, gatewayBinding)) {
                    return runWithDirectModels(
                            task, agent, execution, prompt, estimatedPromptTokens, taskMetricsSample);
                }
                return gatewayBinding
                        .map(binding -> runWithBinding(
                                command, task, agent, execution, binding, prompt, estimatedPromptTokens, taskMetricsSample))
                        .orElseGet(() -> failTaskExecution(
                                task,
                                execution,
                                execution.agentId(),
                                GATEWAY_BINDING_NOT_FOUND,
                                taskMetricsSample));
            }
        } catch (RuntimeException exception) {
            metricsSample.recordFailure();
            throw exception;
        } finally {
            lock.close();
        }
    }

    private boolean shouldUseDirectModelExecution(
            SavedWorkspaceAgent agent,
            Optional<WorkspaceGatewayBinding> gatewayBinding) {
        if (agent.modelProviderId() != null) {
            return true;
        }
        if (agent.openClawAgentId() != null && agent.openClawAgentId().startsWith(DIRECT_MODEL_PREFIX)) {
            return true;
        }
        return gatewayBinding.isEmpty()
                && loadWorkspaceModelProviderPort.findActiveByWorkspaceId(agent.workspaceId()).stream()
                        .anyMatch(WorkspaceModelProvider::isEnabled);
    }

    private RunTaskResult runWithDirectModels(
            TaskDetail task,
            SavedWorkspaceAgent agent,
            SavedTaskExecution execution,
            String prompt,
            int estimatedPromptTokens,
            WorkflowMetrics.OperationSample metricsSample) {
        List<ModelCandidate> candidates = buildModelCandidates(task.workspaceId(), agent);
        if (candidates.isEmpty()) {
            return failTaskExecution(
                    task,
                    execution,
                    agent.agentId(),
                    "no enabled workspace model provider configured",
                    metricsSample);
        }

        updateTaskStatus(task.workspaceId(), task.taskId(), TaskStatus.IN_PROGRESS);
        SavedTaskExecution runningExecution = updateExecutionStatus(
                task.workspaceId(), task.taskId(), execution.executionId(), TaskExecutionStatus.RUNNING, null, null);
        saveExecutionLog(runningExecution, "INFO", "Task execution running via direct model provider");

        String systemPrompt = buildSystemPrompt(agent);
        long startedNanos = System.nanoTime();
        String lastFailure = MODEL_RUN_FAILED;
        String lastModelName = candidates.getFirst().modelId();

        for (int index = 0; index < candidates.size(); index++) {
            ModelCandidate candidate = candidates.get(index);
            lastModelName = candidate.modelId();
            WorkspaceModelRunResult result = runWorkspaceModelPort.run(new WorkspaceModelRunCommand(
                    task.workspaceId(),
                    task.taskId(),
                    execution.executionId(),
                    agent.agentId(),
                    candidate.provider().getProviderKind(),
                    candidate.provider().getWireProtocol(),
                    candidate.provider().getBaseUrl(),
                    candidate.provider().getApiKey(),
                    candidate.modelId(),
                    systemPrompt,
                    prompt));
            if (result.success()) {
                long durationMs = elapsedMillis(startedNanos);
                int actualPromptTokens = result.promptTokens() != null
                        ? Math.max(0, result.promptTokens())
                        : estimatedPromptTokens;
                int actualCompletionTokens = result.completionTokens() != null
                        ? Math.max(0, result.completionTokens())
                        : estimateTokens(result.finalText());
                String usedModel = normalizeModelName(
                        firstNonBlank(result.resolvedModel(), candidate.modelId()));
                return completeTask(
                        task,
                        execution,
                        agent,
                        result.finalText(),
                        durationMs,
                        actualPromptTokens,
                        actualCompletionTokens,
                        usedModel,
                        metricsSample);
            }

            lastFailure = resolveSafeFailure(result.failureReason(), MODEL_RUN_FAILED);
            boolean hasNext = index + 1 < candidates.size();
            if (!result.retryable() || !hasNext) {
                break;
            }
            saveExecutionLog(
                    runningExecution,
                    "WARN",
                    "Model fallback after retryable failure (" + candidate.modelId() + "): " + lastFailure);
        }

        return failTaskExecution(
                task,
                execution,
                agent.agentId(),
                lastFailure,
                elapsedMillis(startedNanos),
                estimatedPromptTokens,
                normalizeModelName(lastModelName),
                metricsSample);
    }

    private List<ModelCandidate> buildModelCandidates(Long workspaceId, SavedWorkspaceAgent agent) {
        List<WorkspaceModelProvider> enabledProviders = loadWorkspaceModelProviderPort
                .findActiveByWorkspaceId(workspaceId)
                .stream()
                .filter(WorkspaceModelProvider::isEnabled)
                .sorted(Comparator
                        .comparingInt(WorkspaceModelProvider::getPriorityOrder)
                        .thenComparing(WorkspaceModelProvider::getId))
                .toList();

        List<ModelCandidate> candidates = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        if (agent.modelProviderId() != null) {
            WorkspaceModelProvider primary = enabledProviders.stream()
                    .filter(provider -> Objects.equals(provider.getId(), agent.modelProviderId()))
                    .findFirst()
                    .orElse(null);
            if (primary != null) {
                String primaryModel = firstNonBlank(agent.modelId(), primary.getDefaultModel());
                addCandidateIfUnique(candidates, seen, primary, primaryModel);
                if (agent.fallbackModelId() != null && !agent.fallbackModelId().isBlank()) {
                    addCandidateIfUnique(candidates, seen, primary, agent.fallbackModelId());
                }
            }
        } else if (!enabledProviders.isEmpty()) {
            WorkspaceModelProvider top = enabledProviders.getFirst();
            addCandidateIfUnique(candidates, seen, top, firstNonBlank(agent.modelId(), top.getDefaultModel()));
            if (agent.fallbackModelId() != null && !agent.fallbackModelId().isBlank()) {
                addCandidateIfUnique(candidates, seen, top, agent.fallbackModelId());
            }
        }

        for (WorkspaceModelProvider provider : enabledProviders) {
            addCandidateIfUnique(candidates, seen, provider, provider.getDefaultModel());
        }
        return candidates;
    }

    private static void addCandidateIfUnique(
            List<ModelCandidate> candidates,
            Set<String> seen,
            WorkspaceModelProvider provider,
            String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return;
        }
        String normalizedModel = modelId.trim();
        String key = provider.getId() + ":" + normalizedModel;
        if (seen.add(key)) {
            candidates.add(new ModelCandidate(provider, normalizedModel));
        }
    }

    private WorkspaceExecutionLock acquireWorkspaceLock(TaskDetail task, SavedWorkspaceAgent agent) {
        return workspaceExecutionLockPort
                .tryAcquire(new WorkspaceExecutionLockCommand(
                        task.workspaceId(),
                        task.taskId(),
                        agent.agentId(),
                        agent.workspacePath()))
                .orElseThrow(() -> new IllegalStateException("workspace path is already running"));
    }

    private RunTaskResult runWithBinding(
            RunTaskCommand command,
            TaskDetail task,
            SavedWorkspaceAgent agent,
            SavedTaskExecution execution,
            WorkspaceGatewayBinding binding,
            String prompt,
            int promptTokens,
            WorkflowMetrics.OperationSample metricsSample) {
        updateTaskStatus(task.workspaceId(), task.taskId(), TaskStatus.IN_PROGRESS);
        SavedTaskExecution runningExecution = updateExecutionStatus(
                task.workspaceId(), task.taskId(), execution.executionId(), TaskExecutionStatus.RUNNING, null, null);
        saveExecutionLog(runningExecution, "INFO", "Task execution running");

        long startedNanos = System.nanoTime();
        try {
            WorkspaceAgentRunResult runResult = runWorkspaceAgentPort.run(new WorkspaceAgentRunCommand(
                    task.workspaceId(),
                    task.taskId(),
                    execution.executionId(),
                    agent.agentId(),
                    agent.openClawAgentId(),
                    agent.workspacePath(),
                    binding.getGatewayUrl(),
                    binding.getToken(),
                    resolveSessionKey(task.workspaceId(), execution.executionId()),
                    prompt,
                    command.createPr()));
            long durationMs = elapsedMillis(startedNanos);
            if (runResult.success()) {
                int completionTokens = estimateTokens(runResult.finalText());
                return completeTask(
                        task,
                        execution,
                        agent,
                        runResult.finalText(),
                        durationMs,
                        promptTokens,
                        completionTokens,
                        defaultModelName,
                        metricsSample);
            }
            return failTaskExecution(
                    task,
                    execution,
                    agent.agentId(),
                    resolveSafeFailure(runResult.failureReason(), GATEWAY_RUN_FAILED),
                    durationMs,
                    promptTokens,
                    defaultModelName,
                    metricsSample);
        } catch (RuntimeException exception) {
            long durationMs = elapsedMillis(startedNanos);
            return failTaskExecution(
                    task,
                    execution,
                    agent.agentId(),
                    resolveSafeFailure(exception.getMessage(), GATEWAY_RUN_FAILED),
                    durationMs,
                    promptTokens,
                    defaultModelName,
                    metricsSample);
        }
    }

    private RunTaskResult completeTask(
            TaskDetail task,
            SavedTaskExecution execution,
            SavedWorkspaceAgent agent,
            String finalText,
            long durationMs,
            int promptTokens,
            int completionTokens,
            String modelName,
            WorkflowMetrics.OperationSample metricsSample) {
        String safeResultSummary = TextTruncation.truncate(finalText, MAX_EXECUTION_TEXT_LENGTH);
        SavedTaskExecution updatedExecution = updateTaskExecutionPort.updateStatus(new TaskExecutionStatusUpdate(
                task.workspaceId(),
                task.taskId(),
                execution.executionId(),
                TaskExecutionStatus.SUCCEEDED,
                safeResultSummary,
                null,
                durationMs,
                promptTokens,
                completionTokens,
                modelName));
        saveExecutionLog(updatedExecution, "INFO", "Task execution succeeded");
        updateTaskStatus(task.workspaceId(), task.taskId(), TaskStatus.COMPLETED);
        metricsSample.recordSuccess();
        return toResult(task.workspaceId(), task.taskId(), updatedExecution, agent.agentId(), TaskStatus.COMPLETED);
    }

    private RunTaskResult failTaskExecution(
            TaskDetail task,
            SavedTaskExecution execution,
            Long agentId,
            String failureReason,
            WorkflowMetrics.OperationSample metricsSample) {
        return failTaskExecution(task, execution, agentId, failureReason, null, null, null, metricsSample);
    }

    private RunTaskResult failTaskExecution(
            TaskDetail task,
            SavedTaskExecution execution,
            Long agentId,
            String failureReason,
            Long durationMs,
            Integer promptTokens,
            String modelName,
            WorkflowMetrics.OperationSample metricsSample) {
        String safeFailureReason = TextTruncation.truncate(
                redactSensitiveTextPort.redact(failureReason), MAX_EXECUTION_TEXT_LENGTH);
        SavedTaskExecution updatedExecution = updateTaskExecutionPort.updateStatus(new TaskExecutionStatusUpdate(
                task.workspaceId(),
                task.taskId(),
                execution.executionId(),
                TaskExecutionStatus.FAILED,
                null,
                safeFailureReason,
                durationMs,
                promptTokens,
                promptTokens == null ? null : 0,
                promptTokens == null ? null : modelName));
        saveExecutionLog(updatedExecution, "ERROR", safeFailureReason);
        updateTaskStatus(task.workspaceId(), task.taskId(), TaskStatus.FAILED);
        metricsSample.recordFailure();
        return toResult(task.workspaceId(), task.taskId(), updatedExecution, agentId, TaskStatus.FAILED);
    }

    private Member requireExistingMember(String googleSub) {
        return loadMemberPort
                .findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
    }

    private GetWorkspaceDetailResult requireJoinedWorkspace(Long workspaceId, String googleSub) {
        return loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
    }

    private long resolveTodayUsedTokens(GetWorkspaceDetailResult workspace) {
        Integer dailyTokenLimit = workspace.dailyTokenLimit();
        if (dailyTokenLimit == null || dailyTokenLimit <= 0) {
            return 0L;
        }
        LocalDateTime startOfTodayUtc = LocalDate.now(clock.withZone(ZoneOffset.UTC)).atStartOfDay();
        return loadTaskExecutionTokenUsagePort.sumTokensSince(workspace.workspaceId(), startOfTodayUtc);
    }

    private static String buildSystemPrompt(SavedWorkspaceAgent agent) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are ").append(agent.name())
                .append(" (category: ").append(agent.category().name()).append(").");
        for (SavedWorkspaceAgentSkillFile file : agent.skillFiles()) {
            if (file.content() == null || file.content().isBlank()) {
                continue;
            }
            if (builder.length() >= MAX_SYSTEM_PROMPT_LENGTH) {
                break;
            }
            builder.append("\n\n[").append(file.fileName()).append("]\n");
            int remaining = MAX_SYSTEM_PROMPT_LENGTH - builder.length();
            String trimmed = file.content().trim();
            builder.append(trimmed.length() <= remaining ? trimmed : trimmed.substring(0, remaining));
        }
        return builder.toString();
    }

    private static long elapsedMillis(long startedNanos) {
        return Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos));
    }

    private static int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, (text.trim().length() + CHARS_PER_TOKEN_ESTIMATE - 1) / CHARS_PER_TOKEN_ESTIMATE);
    }

    private static String normalizeModelName(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return DEFAULT_MODEL_NAME;
        }
        String trimmed = modelName.trim();
        return trimmed.length() <= 120 ? trimmed : trimmed.substring(0, 120);
    }

    private TaskDetail loadTask(Long workspaceId, Long taskId) {
        return loadTaskPort
                .findByWorkspaceIdAndTaskId(workspaceId, taskId)
                .orElseThrow(() -> new IllegalArgumentException("task not found"));
    }

    private void requireRunnableTask(TaskDetail task) {
        if (task.status().isTerminal()) {
            throw new IllegalStateException("terminal task status cannot be run");
        }
    }

    private SavedWorkspaceAgent resolveReadyAgent(TaskDetail task) {
        if (task.assignedAgentId() != null) {
            return loadWorkspaceAgentPort
                    .findActiveByWorkspaceIdAndAgentId(task.workspaceId(), task.assignedAgentId())
                    .filter(this::isExecutableAgent)
                    .orElseThrow(() -> new IllegalArgumentException("ready workspace agent not found"));
        }
        return loadWorkspaceAgentPort.findActiveByWorkspaceId(task.workspaceId()).stream()
                .filter(this::isExecutableAgent)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ready workspace agent not found"));
    }

    private boolean isExecutableAgent(SavedWorkspaceAgent agent) {
        return agent.status() == WorkspaceAgentStatus.READY
                && ((agent.openClawAgentId() != null && !agent.openClawAgentId().isBlank())
                        || agent.modelProviderId() != null);
    }

    private SavedTaskExecution updateExecutionStatus(
            Long workspaceId,
            Long taskId,
            Long executionId,
            TaskExecutionStatus status,
            String resultSummary,
            String failureReason) {
        return updateTaskExecutionPort.updateStatus(new TaskExecutionStatusUpdate(
                workspaceId,
                taskId,
                executionId,
                status,
                resultSummary,
                failureReason));
    }

    private void updateTaskStatus(Long workspaceId, Long taskId, TaskStatus status) {
        updateTaskStatusPort.updateStatus(new TaskStatusUpdate(workspaceId, taskId, status));
    }

    private void saveExecutionLog(SavedTaskExecution execution, String level, String message) {
        saveTaskExecutionLogPort.save(new TaskExecutionLogCommand(
                execution.workspaceId(),
                execution.taskId(),
                execution.executionId(),
                execution.agentId(),
                level,
                TextTruncation.truncate(message, MAX_EXECUTION_TEXT_LENGTH)));
    }

    private RunTaskResult toResult(
            Long workspaceId,
            Long taskId,
            SavedTaskExecution execution,
            Long agentId,
            TaskStatus taskStatus) {
        return new RunTaskResult(
                workspaceId,
                taskId,
                execution.executionId(),
                agentId,
                taskStatus,
                execution.status(),
                execution.resultSummary(),
                execution.failureReason());
    }

    private String resolvePrompt(TaskDetail task) {
        if (task.description() != null && !task.description().isBlank()) {
            return task.description();
        }
        return task.title();
    }

    private String resolveSessionKey(Long workspaceId, Long executionId) {
        return "workspace-" + workspaceId + "-execution-" + executionId;
    }

    private String resolveSafeFailure(String failureReason, String defaultReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return defaultReason;
        }
        String redacted = redactSensitiveTextPort.redact(failureReason.trim());
        return (redacted == null || redacted.isBlank()) ? defaultReason : redacted;
    }

    private String normalizeGoogleSub(String googleSub) {
        if (googleSub == null || googleSub.isBlank()) {
            throw new IllegalArgumentException("requesterGoogleSub must not be blank");
        }
        return googleSub.trim();
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

    private static LoadWorkspaceModelProviderPort noopModelProviderPort() {
        return new LoadWorkspaceModelProviderPort() {
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
        };
    }

    private record ModelCandidate(WorkspaceModelProvider provider, String modelId) {}
}
