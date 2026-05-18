package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.orchestration.OrchestrationStepStatus;
import java.util.List;

public record SavedOrchestrationStep(
        Long stepId,
        String stepKey,
        String instruction,
        Long agentId,
        Long taskId,
        List<String> dependsOn,
        OrchestrationStepStatus status) {

    public SavedOrchestrationStep {
        dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
    }
}
