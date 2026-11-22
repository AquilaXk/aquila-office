package com.aquilaoffice.adapter.in.web.task;

import com.aquilaoffice.application.port.in.CreateTaskResult;
import com.aquilaoffice.application.port.in.GetTaskDetailResult;
import com.aquilaoffice.application.port.in.ListTasksResult;
import com.aquilaoffice.application.port.in.UpdateTaskStatusResult;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import java.time.LocalDateTime;

/**
 * Task HTTP 응답 모델이다.
 */
public record TaskResponse(
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

    public static TaskResponse from(CreateTaskResult result) {
        return new TaskResponse(
                result.taskId(),
                result.workspaceId(),
                result.createdByGoogleSub(),
                result.title(),
                result.description(),
                result.type(),
                result.priority(),
                result.assignedAgentId(),
                result.status(),
                result.createdAt(),
                result.updatedAt());
    }

    public static TaskResponse from(ListTasksResult result) {
        return new TaskResponse(
                result.taskId(),
                result.workspaceId(),
                result.createdByGoogleSub(),
                result.title(),
                result.description(),
                result.type(),
                result.priority(),
                result.assignedAgentId(),
                result.status(),
                result.createdAt(),
                result.updatedAt());
    }

    public static TaskResponse from(GetTaskDetailResult result) {
        return new TaskResponse(
                result.taskId(),
                result.workspaceId(),
                result.createdByGoogleSub(),
                result.title(),
                result.description(),
                result.type(),
                result.priority(),
                result.assignedAgentId(),
                result.status(),
                result.createdAt(),
                result.updatedAt());
    }

    public static TaskResponse from(UpdateTaskStatusResult result) {
        return new TaskResponse(
                result.taskId(),
                result.workspaceId(),
                result.createdByGoogleSub(),
                result.title(),
                result.description(),
                result.type(),
                result.priority(),
                result.assignedAgentId(),
                result.status(),
                result.createdAt(),
                result.updatedAt());
    }
}
