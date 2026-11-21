package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.task.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;

public record SendChatMessageResult(
        Long chatSessionId,
        Long taskId,
        Long workspaceId,
        Long assignedAgentId,
        TaskStatus taskStatus,
        Long taskExecutionId,
        TaskExecutionStatus executionStatus,
        Long orchestrationPlanId,
        String finalText,
        String failureReason,
        LocalDateTime createdAt,
        List<ChatMessageResult> messages) {

    public SendChatMessageResult {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}
