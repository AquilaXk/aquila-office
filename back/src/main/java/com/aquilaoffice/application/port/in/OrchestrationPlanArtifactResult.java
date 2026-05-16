package com.aquilaoffice.application.port.in;

import java.util.List;

public record OrchestrationPlanArtifactResult(
        Long workspaceId,
        Long planId,
        List<OrchestrationPlanStepResult> steps) {}
