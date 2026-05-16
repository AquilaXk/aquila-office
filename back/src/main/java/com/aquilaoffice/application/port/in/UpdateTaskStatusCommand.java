package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.task.TaskStatus;

public record UpdateTaskStatusCommand(
        Long workspaceId,
        Long taskId,
        String requesterGoogleSub,
        TaskStatus status) {}
