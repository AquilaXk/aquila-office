package com.aquilaoffice.adapter.in.web.task;

import com.aquilaoffice.application.port.in.UpdateTaskStatusCommand;
import com.aquilaoffice.domain.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Task 상태 변경 HTTP 요청이다.
 */
public record UpdateTaskStatusRequest(@NotNull TaskStatus status) {

    public UpdateTaskStatusCommand toCommand(Long workspaceId, Long taskId, String requesterGoogleSub) {
        return new UpdateTaskStatusCommand(workspaceId, taskId, requesterGoogleSub, status);
    }
}
