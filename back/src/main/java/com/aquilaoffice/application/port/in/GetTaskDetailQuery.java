package com.aquilaoffice.application.port.in;

public record GetTaskDetailQuery(Long workspaceId, Long taskId, String requesterGoogleSub) {}
