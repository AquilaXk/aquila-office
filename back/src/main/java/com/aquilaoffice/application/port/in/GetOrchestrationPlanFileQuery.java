package com.aquilaoffice.application.port.in;

public record GetOrchestrationPlanFileQuery(
        Long workspaceId,
        Long planId,
        String path,
        String requesterGoogleSub) {}
