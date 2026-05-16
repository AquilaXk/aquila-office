package com.aquilaoffice.adapter.in.web.chat;

import com.aquilaoffice.application.port.in.SendChatMessageCommand;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Chat message 전송 HTTP 요청 모델이다.
 */
public record ChatMessageSendRequest(
        @NotBlank String message,
        @NotNull Long agentId,
        Long repositoryId,
        TaskType taskType,
        TaskPriority priority,
        String title,
        Boolean createPr,
        Long chatSessionId) {

    public SendChatMessageCommand toCommand(Long workspaceId, String requesterGoogleSub) {
        return new SendChatMessageCommand(
                workspaceId,
                requesterGoogleSub,
                message,
                agentId,
                repositoryId,
                taskType,
                priority,
                title,
                Boolean.TRUE.equals(createPr),
                chatSessionId);
    }
}
