package com.aquilaoffice.application.port.in;

public interface GetOrchestrationPlanFileUseCase {

    ArtifactFileResult getOrchestrationPlanFile(GetOrchestrationPlanFileQuery query);
}
