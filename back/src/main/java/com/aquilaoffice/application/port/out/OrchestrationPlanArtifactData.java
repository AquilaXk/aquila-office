package com.aquilaoffice.application.port.out;

import java.util.List;

public record OrchestrationPlanArtifactData(
        Long workspaceId,
        Long planId,
        List<OrchestrationPlanStepData> steps) {}
