package com.aquilaoffice.application.port.out;

public interface LoadOrchestrationPlanArtifactPort {

    OrchestrationPlanArtifactData loadOrchestrationPlan(Long workspaceId, Long planId);
}
