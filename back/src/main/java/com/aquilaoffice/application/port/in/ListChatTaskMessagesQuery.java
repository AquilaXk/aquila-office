package com.aquilaoffice.application.port.in;

public record ListChatTaskMessagesQuery(Long workspaceId, Long taskId, String requesterGoogleSub) {}
