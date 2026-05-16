package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.chat.ChatMessageRole;
import java.time.LocalDateTime;

public record ChatMessageResult(
        Long messageId,
        Long chatSessionId,
        Long taskId,
        Long taskExecutionId,
        ChatMessageRole role,
        String content,
        LocalDateTime createdAt) {}
