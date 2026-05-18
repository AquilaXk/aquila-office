package com.aquilaoffice.adapter.in.web.task;

import com.aquilaoffice.application.port.in.CreateTaskCommand;
import com.aquilaoffice.application.port.in.RunTaskCommand;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskType;
import jakarta.validation.constraints.NotBlank;

/**
 * Task 생성 직후 실행 HTTP 요청이다.
 */
public record CreateAndRunTaskRequest(
        @NotBlank String title,
        String description,
        TaskType type,
        TaskPriority priority,
        Long assignedAgentId,
        Boolean createPr) {

    public CreateTaskCommand toCreateCommand(Long workspaceId, String requesterGoogleSub) {
        return new CreateTaskCommand(
                workspaceId,
                requesterGoogleSub,
                title.trim(),
                normalize(description),
                type,
                priority,
                assignedAgentId);
    }

    public RunTaskCommand toRunCommand(Long workspaceId, Long taskId, String requesterGoogleSub) {
        return new RunTaskCommand(workspaceId, taskId, requesterGoogleSub, Boolean.TRUE.equals(createPr));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
