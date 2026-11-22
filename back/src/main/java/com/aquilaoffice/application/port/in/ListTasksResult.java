package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import java.time.LocalDateTime;

public record ListTasksResult(
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
        LocalDateTime updatedAt) {

    public ListTasksResult(
            Long taskId,
            Long workspaceId,
            String createdByGoogleSub,
            String title,
            TaskType type,
            TaskPriority priority,
            Long assignedAgentId,
            TaskStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this(
                taskId,
                workspaceId,
                createdByGoogleSub,
                title,
                null,
                type,
                priority,
                assignedAgentId,
                status,
                createdAt,
                updatedAt);
    }
}
