package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskType;

/**
 * Chat message를 Task 실행 입력으로 해석할 때 AI adapter에 전달하는 command다.
 */
public record AnalyzeChatIntentCommand(
        Long workspaceId,
        String message,
        Long agentId,
        Long repositoryId,
        TaskType requestedTaskType,
        TaskPriority requestedPriority,
        String requestedTitle,
        Boolean requestedCreatePr) {}
