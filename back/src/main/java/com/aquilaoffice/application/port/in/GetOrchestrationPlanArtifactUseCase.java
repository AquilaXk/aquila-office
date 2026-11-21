package com.aquilaoffice.application.port.in;

public interface GetOrchestrationPlanArtifactUseCase {

    OrchestrationPlanArtifactResult getOrchestrationPlanArtifact(GetOrchestrationPlanArtifactQuery query);
}
