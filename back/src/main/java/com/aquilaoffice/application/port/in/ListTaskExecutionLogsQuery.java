package com.aquilaoffice.application.port.in;

public record ListTaskExecutionLogsQuery(Long workspaceId, Long taskId, String requesterGoogleSub) {}
