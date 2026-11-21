package com.aquilaoffice.adapter.in.web.artifact;

import com.aquilaoffice.application.port.in.OrchestrationPlanArtifactResult;
import java.util.List;

public record OrchestrationPlanArtifactResponse(
        Long workspaceId,
        Long planId,
        List<OrchestrationPlanStepResponse> steps) {

    static OrchestrationPlanArtifactResponse from(OrchestrationPlanArtifactResult result) {
        return new OrchestrationPlanArtifactResponse(
                result.workspaceId(),
                result.planId(),
                result.steps().stream().map(OrchestrationPlanStepResponse::from).toList());
    }
}
