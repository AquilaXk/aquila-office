package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CancelTaskCommand;
import com.aquilaoffice.application.port.in.CancelTaskUseCase;
import com.aquilaoffice.application.port.in.UpdateTaskStatusResult;
import com.aquilaoffice.application.port.out.CancelTaskExecutionsPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionReportsPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionLogPort;
import com.aquilaoffice.application.port.out.SavedTaskExecution;
import com.aquilaoffice.application.port.out.TaskDetail;
import com.aquilaoffice.application.port.out.TaskExecutionLogCommand;
import com.aquilaoffice.application.port.out.TaskStatusUpdate;
import com.aquilaoffice.application.port.out.UpdateTaskStatusPort;
import com.aquilaoffice.domain.task.Task;
import com.aquilaoffice.domain.task.TaskStatus;
import java.util.List;
import java.util.Objects;

/**
 * 워크스페이스 멤버가 비터미널 Task와 활성 실행을 취소할 수 있게 처리하는 application service다.
 */
public class CancelTaskService implements CancelTaskUseCase {

    private static final String CANCEL_LOG_MESSAGE = "Task canceled by member";

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadTaskPort loadTaskPort;
    private final UpdateTaskStatusPort updateTaskStatusPort;
    private final CancelTaskExecutionsPort cancelTaskExecutionsPort;
    private final LoadTaskExecutionReportsPort loadTaskExecutionReportsPort;
    private final SaveTaskExecutionLogPort saveTaskExecutionLogPort;

    public CancelTaskService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort,
            UpdateTaskStatusPort updateTaskStatusPort,
            CancelTaskExecutionsPort cancelTaskExecutionsPort,
            LoadTaskExecutionReportsPort loadTaskExecutionReportsPort,
            SaveTaskExecutionLogPort saveTaskExecutionLogPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "loadTaskPort must not be null");
        this.updateTaskStatusPort =
                Objects.requireNonNull(updateTaskStatusPort, "updateTaskStatusPort must not be null");
        this.cancelTaskExecutionsPort =
                Objects.requireNonNull(cancelTaskExecutionsPort, "cancelTaskExecutionsPort must not be null");
        this.loadTaskExecutionReportsPort = Objects.requireNonNull(
                loadTaskExecutionReportsPort, "loadTaskExecutionReportsPort must not be null");
        this.saveTaskExecutionLogPort =
                Objects.requireNonNull(saveTaskExecutionLogPort, "saveTaskExecutionLogPort must not be null");
    }

    @Override
    public UpdateTaskStatusResult cancelTask(CancelTaskCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        requireJoinedWorkspace(command.workspaceId(), command.requesterGoogleSub());

        TaskDetail taskDetail = loadTaskPort
                .findByWorkspaceIdAndTaskId(command.workspaceId(), command.taskId())
                .orElseThrow(() -> new IllegalArgumentException("task not found"));
        Task task = restoreTask(taskDetail);
        task.changeStatus(TaskStatus.CANCELED);

        List<SavedTaskExecution> canceledExecutions = cancelTaskExecutionsPort.cancelActiveByWorkspaceIdAndTaskId(
                command.workspaceId(),
                command.taskId(),
                CANCEL_LOG_MESSAGE);
        recordCancelLogs(command.workspaceId(), command.taskId(), canceledExecutions);

        TaskDetail updatedTask = updateTaskStatusPort.updateStatus(
                new TaskStatusUpdate(command.workspaceId(), command.taskId(), TaskStatus.CANCELED));
        return TaskResultMapper.toStatusResult(updatedTask);
    }

    private void recordCancelLogs(Long workspaceId, Long taskId, List<SavedTaskExecution> canceledExecutions) {
        if (!canceledExecutions.isEmpty()) {
            for (SavedTaskExecution execution : canceledExecutions) {
                saveWarnLog(execution);
            }
            return;
        }
        loadTaskExecutionReportsPort.findByWorkspaceIdAndTaskIdOrderByCreatedAtDesc(workspaceId, taskId)
                .stream()
                .findFirst()
                .ifPresent(this::saveWarnLog);
    }

    private void saveWarnLog(SavedTaskExecution execution) {
        saveTaskExecutionLogPort.save(new TaskExecutionLogCommand(
                execution.workspaceId(),
                execution.taskId(),
                execution.executionId(),
                execution.agentId(),
                "WARN",
                CANCEL_LOG_MESSAGE));
    }

    private void requireJoinedWorkspace(Long workspaceId, String requesterGoogleSub) {
        loadMemberPort.findByGoogleSub(requesterGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
        loadWorkspacePort.findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, requesterGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
    }

    private static Task restoreTask(TaskDetail detail) {
        return Task.restore(
                detail.taskId(),
                detail.workspaceId(),
                detail.createdByGoogleSub(),
                detail.title(),
                detail.description(),
                detail.type(),
                detail.priority(),
                detail.assignedAgentId(),
                detail.status(),
                detail.sourceRef());
    }
}
