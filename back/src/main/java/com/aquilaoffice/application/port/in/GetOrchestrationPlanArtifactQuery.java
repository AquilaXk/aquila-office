package com.aquilaoffice.application.port.in;

public record GetOrchestrationPlanArtifactQuery(
        Long workspaceId,
        Long planId,
        String requesterGoogleSub) {}
