package com.aquilaoffice.adapter.in.web.chat;

import com.aquilaoffice.application.port.in.ChatMessageResult;
import com.aquilaoffice.domain.chat.ChatMessageRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * Chat message HTTP 응답 모델이다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessageResponse(
        Long messageId,
        Long chatSessionId,
        Long taskId,
        Long taskExecutionId,
        ChatMessageRole role,
        String content,
        LocalDateTime createdAt) {

    public static ChatMessageResponse from(ChatMessageResult result) {
        return new ChatMessageResponse(
                result.messageId(),
                result.chatSessionId(),
                result.taskId(),
                result.taskExecutionId(),
                result.role(),
                result.content(),
                result.createdAt());
    }
}
