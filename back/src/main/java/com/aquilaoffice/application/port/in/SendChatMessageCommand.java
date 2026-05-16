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
        boolean createPr,
        Long chatSessionId) {}
