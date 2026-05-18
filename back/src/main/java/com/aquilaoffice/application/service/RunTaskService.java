package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.RunTaskCommand;
import com.aquilaoffice.application.port.in.RunTaskResult;
import com.aquilaoffice.application.port.in.RunTaskUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
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
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.execution.TaskExecution;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.task.TaskStatus;
import java.util.Objects;

/**
 * 기존 Task를 READY Agent와 Gateway binding으로 실행하고 실행/작업 상태를 갱신한다.
 */
public class RunTaskService implements RunTaskUseCase {

    private static final String GATEWAY_BINDING_NOT_FOUND = "workspace gateway binding not found";
    private static final String GATEWAY_RUN_FAILED = "workspace agent run failed";

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
    }

    @Override
    public RunTaskResult runTask(RunTaskCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String requesterGoogleSub = normalizeGoogleSub(command.requesterGoogleSub());
        Member requester = requireExistingMember(requesterGoogleSub);
        requireJoinedWorkspace(command.workspaceId(), requester.getGoogleSub());
        TaskDetail task = loadTask(command.workspaceId(), command.taskId());
        requireRunnableTask(task);
        SavedWorkspaceAgent agent = resolveReadyAgent(task);
        SavedTaskExecution execution =
                saveTaskExecutionPort.save(TaskExecution.create(command.workspaceId(), command.taskId(), agent.agentId()));
        saveExecutionLog(execution, "INFO", "Task execution queued");

        return loadWorkspaceGatewayBindingPort
                .findActiveByWorkspaceId(command.workspaceId())
                .map(binding -> runWithBinding(command, task, agent, execution, binding))
                .orElseGet(() -> failBeforeGateway(task, execution, GATEWAY_BINDING_NOT_FOUND));
    }

    private RunTaskResult runWithBinding(
            RunTaskCommand command,
            TaskDetail task,
            SavedWorkspaceAgent agent,
            SavedTaskExecution execution,
            WorkspaceGatewayBinding binding) {
        updateTaskStatus(task.workspaceId(), task.taskId(), TaskStatus.IN_PROGRESS);
        SavedTaskExecution runningExecution = updateExecutionStatus(
                task.workspaceId(), task.taskId(), execution.executionId(), TaskExecutionStatus.RUNNING, null, null);
        saveExecutionLog(runningExecution, "INFO", "Task execution running");

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
                    resolvePrompt(task),
                    command.createPr()));
            if (runResult.success()) {
                return completeTask(task, execution, agent, runResult.finalText());
            }
            return failAfterGateway(task, execution, agent, resolveFailureReason(runResult.failureReason()));
        } catch (RuntimeException exception) {
            return failAfterGateway(task, execution, agent, resolveFailureReason(exception.getMessage()));
        }
    }

    private RunTaskResult completeTask(
            TaskDetail task,
            SavedTaskExecution execution,
            SavedWorkspaceAgent agent,
            String finalText) {
        SavedTaskExecution updatedExecution = updateExecutionStatus(
                task.workspaceId(),
                task.taskId(),
                execution.executionId(),
                TaskExecutionStatus.SUCCEEDED,
                finalText,
                null);
        saveExecutionLog(updatedExecution, "INFO", "Task execution succeeded");
        updateTaskStatus(task.workspaceId(), task.taskId(), TaskStatus.COMPLETED);
        return toResult(task.workspaceId(), task.taskId(), updatedExecution, agent.agentId(), TaskStatus.COMPLETED);
    }

    private RunTaskResult failBeforeGateway(TaskDetail task, SavedTaskExecution execution, String failureReason) {
        SavedTaskExecution updatedExecution = updateExecutionStatus(
                task.workspaceId(),
                task.taskId(),
                execution.executionId(),
                TaskExecutionStatus.FAILED,
                null,
                failureReason);
        saveExecutionLog(updatedExecution, "ERROR", failureReason);
        updateTaskStatus(task.workspaceId(), task.taskId(), TaskStatus.FAILED);
        return toResult(task.workspaceId(), task.taskId(), updatedExecution, execution.agentId(), TaskStatus.FAILED);
    }

    private RunTaskResult failAfterGateway(
            TaskDetail task,
            SavedTaskExecution execution,
            SavedWorkspaceAgent agent,
            String failureReason) {
        SavedTaskExecution updatedExecution = updateExecutionStatus(
                task.workspaceId(),
                task.taskId(),
                execution.executionId(),
                TaskExecutionStatus.FAILED,
                null,
                failureReason);
        saveExecutionLog(updatedExecution, "ERROR", failureReason);
        updateTaskStatus(task.workspaceId(), task.taskId(), TaskStatus.FAILED);
        return toResult(task.workspaceId(), task.taskId(), updatedExecution, agent.agentId(), TaskStatus.FAILED);
    }

    private Member requireExistingMember(String googleSub) {
        return loadMemberPort
                .findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
    }

    private void requireJoinedWorkspace(Long workspaceId, String googleSub) {
        loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
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
                message));
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
