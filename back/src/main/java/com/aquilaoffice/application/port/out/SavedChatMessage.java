package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.chat.ChatMessageRole;
import java.time.LocalDateTime;

public record SavedChatMessage(
        Long messageId,
        Long workspaceId,
        Long chatSessionId,
        Long taskId,
        Long taskExecutionId,
        ChatMessageRole role,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
