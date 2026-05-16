package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.UpdateTaskStatusCommand;
import com.aquilaoffice.application.port.in.UpdateTaskStatusResult;
import com.aquilaoffice.application.port.in.UpdateTaskStatusUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.TaskDetail;
import com.aquilaoffice.application.port.out.TaskStatusUpdate;
import com.aquilaoffice.application.port.out.UpdateTaskStatusPort;
import com.aquilaoffice.domain.task.Task;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.util.Objects;

/**
 * Task 상태 변경은 워크스페이스 ADMIN에게만 허용하고 도메인 전이 규칙을 적용한다.
 */
public class UpdateTaskStatusService implements UpdateTaskStatusUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadTaskPort loadTaskPort;
    private final UpdateTaskStatusPort updateTaskStatusPort;

    public UpdateTaskStatusService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort,
            UpdateTaskStatusPort updateTaskStatusPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "loadTaskPort must not be null");
        this.updateTaskStatusPort =
                Objects.requireNonNull(updateTaskStatusPort, "updateTaskStatusPort must not be null");
    }

    @Override
    public UpdateTaskStatusResult updateTaskStatus(UpdateTaskStatusCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String requesterGoogleSub = normalizeGoogleSub(command.requesterGoogleSub());
        requireExistingMember(requesterGoogleSub);
        requireAdminMembership(command.workspaceId(), requesterGoogleSub);

        TaskDetail taskDetail = loadTaskPort
                .findByWorkspaceIdAndTaskId(command.workspaceId(), command.taskId())
                .orElseThrow(() -> new IllegalArgumentException("task not found"));
        Task task = Task.restore(
                taskDetail.taskId(),
                taskDetail.workspaceId(),
                taskDetail.createdByGoogleSub(),
                taskDetail.title(),
                taskDetail.description(),
                taskDetail.type(),
                taskDetail.priority(),
                taskDetail.assignedAgentId(),
                taskDetail.status());
        task.changeStatus(command.status());

        return TaskResultMapper.toStatusResult(updateTaskStatusPort.updateStatus(
                new TaskStatusUpdate(command.workspaceId(), command.taskId(), task.getStatus())));
    }

    private void requireExistingMember(String googleSub) {
        loadMemberPort
                .findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
    }

    private void requireAdminMembership(Long workspaceId, String googleSub) {
        var workspace = loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
        if (workspace.myRole() != WorkspaceMemberRole.ADMIN) {
            throw new IllegalArgumentException("workspace admin membership required");
        }
    }

    private String normalizeGoogleSub(String googleSub) {
        if (googleSub == null || googleSub.isBlank()) {
            throw new IllegalArgumentException("requesterGoogleSub must not be blank");
        }
        return googleSub.trim();
    }
}
