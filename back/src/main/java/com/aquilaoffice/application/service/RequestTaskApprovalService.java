package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.RequestTaskApprovalCommand;
import com.aquilaoffice.application.port.in.RequestTaskApprovalUseCase;
import com.aquilaoffice.application.port.in.UpdateTaskStatusResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionReportsPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionLogPort;
import com.aquilaoffice.application.port.out.TaskDetail;
import com.aquilaoffice.application.port.out.TaskExecutionLogCommand;
import com.aquilaoffice.application.port.out.TaskStatusUpdate;
import com.aquilaoffice.application.port.out.UpdateTaskStatusPort;
import com.aquilaoffice.domain.task.Task;
import com.aquilaoffice.domain.task.TaskStatus;
import java.util.Objects;

/**
 * 실행 대기/진행 중인 Task를 사용자 승인 대기(WAITING_USER) 상태로 전환한다.
 */
public class RequestTaskApprovalService implements RequestTaskApprovalUseCase {

    private static final int MAX_LOG_LENGTH = 1000;

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadTaskPort loadTaskPort;
    private final UpdateTaskStatusPort updateTaskStatusPort;
    private final LoadTaskExecutionReportsPort loadTaskExecutionReportsPort;
    private final SaveTaskExecutionLogPort saveTaskExecutionLogPort;

    public RequestTaskApprovalService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort,
            UpdateTaskStatusPort updateTaskStatusPort,
            LoadTaskExecutionReportsPort loadTaskExecutionReportsPort,
            SaveTaskExecutionLogPort saveTaskExecutionLogPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "loadTaskPort must not be null");
        this.updateTaskStatusPort =
                Objects.requireNonNull(updateTaskStatusPort, "updateTaskStatusPort must not be null");
        this.loadTaskExecutionReportsPort = Objects.requireNonNull(
                loadTaskExecutionReportsPort, "loadTaskExecutionReportsPort must not be null");
        this.saveTaskExecutionLogPort =
                Objects.requireNonNull(saveTaskExecutionLogPort, "saveTaskExecutionLogPort must not be null");
    }

    @Override
    public UpdateTaskStatusResult requestApproval(RequestTaskApprovalCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        requireJoinedWorkspace(command.workspaceId(), command.requesterGoogleSub());

        TaskDetail taskDetail = loadTaskPort
                .findByWorkspaceIdAndTaskId(command.workspaceId(), command.taskId())
                .orElseThrow(() -> new IllegalArgumentException("task not found"));
        if (!isRequestableStatus(taskDetail.status())) {
            throw new IllegalStateException("task cannot request approval in current status");
        }
        Task task = Task.restore(
                taskDetail.taskId(),
                taskDetail.workspaceId(),
                taskDetail.createdByGoogleSub(),
                taskDetail.title(),
                taskDetail.description(),
                taskDetail.type(),
                taskDetail.priority(),
                taskDetail.assignedAgentId(),
                taskDetail.status(),
                taskDetail.sourceRef());
        task.changeStatus(TaskStatus.WAITING_USER);

        String message = command.reason() == null
                ? "Approval requested"
                : TextTruncation.truncate("Approval requested: " + command.reason(), MAX_LOG_LENGTH);
        loadTaskExecutionReportsPort.findByWorkspaceIdAndTaskIdOrderByCreatedAtDesc(
                        command.workspaceId(), command.taskId())
                .stream()
                .findFirst()
                .ifPresent(execution -> saveTaskExecutionLogPort.save(new TaskExecutionLogCommand(
                        execution.workspaceId(),
                        execution.taskId(),
                        execution.executionId(),
                        execution.agentId(),
                        "INFO",
                        message)));

        TaskDetail updatedTask = updateTaskStatusPort.updateStatus(
                new TaskStatusUpdate(command.workspaceId(), command.taskId(), TaskStatus.WAITING_USER));
        return TaskResultMapper.toStatusResult(updatedTask);
    }

    private static boolean isRequestableStatus(TaskStatus status) {
        return status == TaskStatus.REQUESTED
                || status == TaskStatus.ASSIGNED
                || status == TaskStatus.IN_PROGRESS;
    }

    private void requireJoinedWorkspace(Long workspaceId, String requesterGoogleSub) {
        loadMemberPort.findByGoogleSub(requesterGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
        loadWorkspacePort.findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, requesterGoogleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
    }
}
