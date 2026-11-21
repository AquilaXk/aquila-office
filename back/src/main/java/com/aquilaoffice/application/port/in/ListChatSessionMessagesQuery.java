package com.aquilaoffice.application.port.in;

public record ListChatSessionMessagesQuery(
        Long workspaceId,
        Long chatSessionId,
        String requesterGoogleSub,
        Long afterMessageId,
        int limit) {}
