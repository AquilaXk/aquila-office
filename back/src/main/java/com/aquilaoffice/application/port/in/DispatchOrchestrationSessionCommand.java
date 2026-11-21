package com.aquilaoffice.application.port.in;

public record DispatchOrchestrationSessionCommand(
        Long workspaceId,
        Long sessionId,
        String requesterGoogleSub,
        boolean createPr) {}
