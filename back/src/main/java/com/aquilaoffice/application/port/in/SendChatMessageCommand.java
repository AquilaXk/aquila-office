package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskType;

public record SendChatMessageCommand(
        Long workspaceId,
        String requesterGoogleSub,
        String message,
        Long agentId,
        Long repositoryId,
        TaskType taskType,
        TaskPriority priority,
        String title,
        Boolean createPr,
        Long chatSessionId,
        String sourceRef) {

    public SendChatMessageCommand(
            Long workspaceId,
            String requesterGoogleSub,
            String message,
            Long agentId,
            Long repositoryId,
            TaskType taskType,
            TaskPriority priority,
            String title,
            Boolean createPr,
            Long chatSessionId) {
        this(
                workspaceId,
                requesterGoogleSub,
                message,
                agentId,
                repositoryId,
                taskType,
                priority,
                title,
                createPr,
                chatSessionId,
                null);
    }
}
