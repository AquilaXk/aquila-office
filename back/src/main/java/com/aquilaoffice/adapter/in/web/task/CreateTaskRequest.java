package com.aquilaoffice.adapter.in.web.task;

import com.aquilaoffice.application.port.in.CreateTaskCommand;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskType;
import jakarta.validation.constraints.NotBlank;

/**
 * Task 생성 HTTP 요청이다.
 */
public record CreateTaskRequest(
        @NotBlank String title,
        String description,
        TaskType type,
        TaskPriority priority,
        Long assignedAgentId) {

    public CreateTaskCommand toCommand(Long workspaceId, String requesterGoogleSub) {
        return new CreateTaskCommand(
                workspaceId,
                requesterGoogleSub,
                title.trim(),
                normalize(description),
                type,
                priority,
                assignedAgentId);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
