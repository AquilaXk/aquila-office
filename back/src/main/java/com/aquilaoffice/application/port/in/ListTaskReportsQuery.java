package com.aquilaoffice.application.port.in;

public record ListTaskReportsQuery(
        Long workspaceId,
        Long taskId,
        String requesterGoogleSub) {}
