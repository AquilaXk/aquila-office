package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import java.time.LocalDateTime;

public record CreateTaskResult(
        Long taskId,
        Long workspaceId,
        String createdByGoogleSub,
        String title,
        String description,
        TaskType type,
        TaskPriority priority,
        Long assignedAgentId,
        TaskStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
