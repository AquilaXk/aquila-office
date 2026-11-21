package com.aquilaoffice.application.port.out;

import java.time.LocalDateTime;

public record SavedChatSession(
        Long chatSessionId,
        Long workspaceId,
        String createdByGoogleSub,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
