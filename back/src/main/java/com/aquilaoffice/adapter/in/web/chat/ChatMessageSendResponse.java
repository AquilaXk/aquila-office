package com.aquilaoffice.adapter.in.web.chat;

import com.aquilaoffice.application.port.in.SendChatMessageResult;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.task.TaskStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Chat message 전송 결과 HTTP 응답 모델이다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessageSendResponse(
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
        List<ChatMessageResponse> messages) {

    public static ChatMessageSendResponse from(SendChatMessageResult result) {
        return new ChatMessageSendResponse(
                result.chatSessionId(),
                result.taskId(),
                result.workspaceId(),
                result.assignedAgentId(),
                result.taskStatus(),
                result.taskExecutionId(),
                result.executionStatus(),
                result.orchestrationPlanId(),
                result.finalText(),
                result.failureReason(),
                result.createdAt(),
                result.messages().stream().map(ChatMessageResponse::from).toList());
    }
}
