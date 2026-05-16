package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ChatMessageResult;
import com.aquilaoffice.application.port.out.SavedChatMessage;

final class ChatMessageResultMapper {

    private ChatMessageResultMapper() {}

    static ChatMessageResult toResult(SavedChatMessage message) {
        return new ChatMessageResult(
                message.messageId(),
                message.chatSessionId(),
                message.taskId(),
                message.taskExecutionId(),
                message.role(),
                message.content(),
                message.createdAt());
    }
}
