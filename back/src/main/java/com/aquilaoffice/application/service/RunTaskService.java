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
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.RunWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionLogPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionPort;
import com.aquilaoffice.application.port.out.SavedTaskExecution;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgent;
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
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.execution.TaskExecution;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.task.TaskStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 기존 Task를 READY Agent와 Gateway binding으로 실행하고 실행/작업 상태를 갱신한다.
 */
public class RunTaskService implements RunTaskUseCase {

    private static final String GATEWAY_BINDING_NOT_FOUND = "workspace gateway binding not found";
    private static final String GATEWAY_RUN_FAILED = "workspace agent run failed";
    private static final String DAILY_TOKEN_LIMIT_EXCEEDED = "workspace daily token limit exceeded";
    private static final String DEFAULT_MODEL_NAME = "gemini-2.5-flash";
    private static final int MAX_EXECUTION_TEXT_LENGTH = 1000;
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

            if (isDailyTokenLimitExceeded(workspace)) {
                return failTaskExecution(
                        task,
                        execution,
                        execution.agentId(),
                        DAILY_TOKEN_LIMIT_EXCEEDED,
                        taskMetricsSample);
            }

            return loadWorkspaceGatewayBindingPort
                    .findActiveByWorkspaceId(command.workspaceId())
                    .map(binding -> runWithBinding(command, task, agent, execution, binding, taskMetricsSample))
                    .orElseGet(() -> failTaskExecution(
                            task,
                            execution,
                            execution.agentId(),
                            GATEWAY_BINDING_NOT_FOUND,
                            taskMetricsSample));
        } catch (RuntimeException exception) {
            metricsSample.recordFailure();
            throw exception;
        } finally {
            lock.close();
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
            WorkflowMetrics.OperationSample metricsSample) {
        updateTaskStatus(task.workspaceId(), task.taskId(), TaskStatus.IN_PROGRESS);
        SavedTaskExecution runningExecution = updateExecutionStatus(
                task.workspaceId(), task.taskId(), execution.executionId(), TaskExecutionStatus.RUNNING, null, null);
        saveExecutionLog(runningExecution, "INFO", "Task execution running");

        String prompt = resolvePrompt(task);
        int promptTokens = estimateTokens(prompt);
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
                        metricsSample);
            }
            return failTaskExecution(
                    task,
                    execution,
                    agent.agentId(),
                    resolveFailureReason(runResult.failureReason()),
                    durationMs,
                    promptTokens,
                    metricsSample);
        } catch (RuntimeException exception) {
            long durationMs = elapsedMillis(startedNanos);
            return failTaskExecution(
                    task,
                    execution,
                    agent.agentId(),
                    resolveFailureReason(exception.getMessage()),
                    durationMs,
                    promptTokens,
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
                defaultModelName));
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
        return failTaskExecution(task, execution, agentId, failureReason, null, null, metricsSample);
    }

    private RunTaskResult failTaskExecution(
            TaskDetail task,
            SavedTaskExecution execution,
            Long agentId,
            String failureReason,
            Long durationMs,
            Integer promptTokens,
            WorkflowMetrics.OperationSample metricsSample) {
        String safeFailureReason = TextTruncation.truncate(failureReason, MAX_EXECUTION_TEXT_LENGTH);
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
                promptTokens == null ? null : defaultModelName));
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

    private boolean isDailyTokenLimitExceeded(GetWorkspaceDetailResult workspace) {
        Integer dailyTokenLimit = workspace.dailyTokenLimit();
        if (dailyTokenLimit == null || dailyTokenLimit <= 0) {
            return false;
        }
        LocalDateTime startOfTodayUtc = LocalDate.now(clock.withZone(ZoneOffset.UTC)).atStartOfDay();
        long todayUsedTokens = loadTaskExecutionTokenUsagePort.sumTokensSince(workspace.workspaceId(), startOfTodayUtc);
        return todayUsedTokens >= dailyTokenLimit;
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
                && agent.openClawAgentId() != null
                && !agent.openClawAgentId().isBlank();
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

    private String resolveFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return GATEWAY_RUN_FAILED;
        }
        return failureReason.trim();
    }

    private String normalizeGoogleSub(String googleSub) {
        if (googleSub == null || googleSub.isBlank()) {
            throw new IllegalArgumentException("requesterGoogleSub must not be blank");
        }
        return googleSub.trim();
    }
}
