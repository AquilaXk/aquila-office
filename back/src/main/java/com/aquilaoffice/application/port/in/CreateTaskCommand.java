package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskType;

public record CreateTaskCommand(
        Long workspaceId,
        String requesterGoogleSub,
        String title,
        String description,
        TaskType type,
        TaskPriority priority,
        Long assignedAgentId) {}
