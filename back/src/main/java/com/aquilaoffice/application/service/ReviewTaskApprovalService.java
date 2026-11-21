package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ReviewTaskApprovalCommand;
import com.aquilaoffice.application.port.in.ReviewTaskApprovalUseCase;
import com.aquilaoffice.application.port.in.RunTaskCommand;
import com.aquilaoffice.application.port.in.RunTaskUseCase;
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
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.util.List;
import java.util.Objects;

/**
 * WAITING_USER 상태의 Task 승인(APPROVE) 또는 반려(REJECT)를 처리하는 application service다.
 */
public class ReviewTaskApprovalService implements ReviewTaskApprovalUseCase {

    private static final int MAX_LOG_LENGTH = 1000;

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadTaskPort loadTaskPort;
    private final UpdateTaskStatusPort updateTaskStatusPort;
    private final CancelTaskExecutionsPort cancelTaskExecutionsPort;
    private final LoadTaskExecutionReportsPort loadTaskExecutionReportsPort;
    private final SaveTaskExecutionLogPort saveTaskExecutionLogPort;
    private final RunTaskUseCase runTaskUseCase;

    public ReviewTaskApprovalService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort,
            UpdateTaskStatusPort updateTaskStatusPort,
            CancelTaskExecutionsPort cancelTaskExecutionsPort,
            LoadTaskExecutionReportsPort loadTaskExecutionReportsPort,
            SaveTaskExecutionLogPort saveTaskExecutionLogPort,
            RunTaskUseCase runTaskUseCase) {
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
        this.runTaskUseCase = Objects.requireNonNull(runTaskUseCase, "runTaskUseCase must not be null");
    }

    @Override
    public UpdateTaskStatusResult reviewApproval(ReviewTaskApprovalCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        requireAdminWorkspace(command.workspaceId(), command.requesterGoogleSub());

        TaskDetail taskDetail = loadTaskPort
                .findByWorkspaceIdAndTaskId(command.workspaceId(), command.taskId())
                .orElseThrow(() -> new IllegalArgumentException("task not found"));
        if (taskDetail.status() != TaskStatus.WAITING_USER) {
            throw new IllegalStateException("task is not waiting user approval");
        }

        if (command.isApprove()) {
            return approveTask(command);
        }
        return rejectTask(command);
    }

    private UpdateTaskStatusResult approveTask(ReviewTaskApprovalCommand command) {
        String message = formatLogMessage("Approval granted", command.comment());
        logLatestExecution(command.workspaceId(), command.taskId(), "INFO", message);

        TaskDetail updatedTask = updateTaskStatusPort.updateStatus(
                new TaskStatusUpdate(command.workspaceId(), command.taskId(), TaskStatus.IN_PROGRESS));
        if (command.runAfterApprove()) {
            runTaskUseCase.runTask(new RunTaskCommand(
                    command.workspaceId(),
                    command.taskId(),
                    command.requesterGoogleSub(),
                    false));
            updatedTask = loadTaskPort
                    .findByWorkspaceIdAndTaskId(command.workspaceId(), command.taskId())
                    .orElse(updatedTask);
        }
        return TaskResultMapper.toStatusResult(updatedTask);
    }

    private UpdateTaskStatusResult rejectTask(ReviewTaskApprovalCommand command) {
        String message = formatLogMessage("Approval rejected", command.comment());
        List<SavedTaskExecution> canceledExecutions = cancelTaskExecutionsPort.cancelActiveByWorkspaceIdAndTaskId(
                command.workspaceId(),
                command.taskId(),
                message);
        if (!canceledExecutions.isEmpty()) {
            for (SavedTaskExecution execution : canceledExecutions) {
                saveLog(execution, "WARN", message);
            }
        } else {
            logLatestExecution(command.workspaceId(), command.taskId(), "WARN", message);
        }

        TaskDetail updatedTask = updateTaskStatusPort.updateStatus(
                new TaskStatusUpdate(command.workspaceId(), command.taskId(), TaskStatus.CANCELED));
        return TaskResultMapper.toStatusResult(updatedTask);
    }

    private void logLatestExecution(Long workspaceId, Long taskId, String level, String message) {
        loadTaskExecutionReportsPort.findByWorkspaceIdAndTaskIdOrderByCreatedAtDesc(workspaceId, taskId)
                .stream()
                .findFirst()
                .ifPresent(execution -> saveLog(execution, level, message));
    }

    private void saveLog(SavedTaskExecution execution, String level, String message) {
        saveTaskExecutionLogPort.save(new TaskExecutionLogCommand(
                execution.workspaceId(),
                execution.taskId(),
                execution.executionId(),
                execution.agentId(),
                level,
                message));
    }

    private static String formatLogMessage(String prefix, String comment) {
        if (comment == null) {
            return prefix;
        }
        return TextTruncation.truncate(prefix + ": " + comment, MAX_LOG_LENGTH);
    }

    private void requireAdminWorkspace(Long workspaceId, String requesterGoogleSub) {
        loadMemberPort.findByGoogleSub(requesterGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
        GetWorkspaceDetailResult workspace = loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, requesterGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
        if (workspace.myRole() != WorkspaceMemberRole.ADMIN) {
            throw new IllegalArgumentException("workspace admin membership required");
        }
    }
}
