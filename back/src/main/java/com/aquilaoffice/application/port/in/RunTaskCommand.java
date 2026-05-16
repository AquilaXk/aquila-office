package com.aquilaoffice.application.port.in;

public record RunTaskCommand(Long workspaceId, Long taskId, String requesterGoogleSub, boolean createPr) {}
