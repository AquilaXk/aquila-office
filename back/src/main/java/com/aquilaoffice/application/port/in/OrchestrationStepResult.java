package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.orchestration.OrchestrationStepStatus;
import java.util.List;

public record OrchestrationStepResult(
        String stepKey,
        String instruction,
        Long agentId,
        Long taskId,
        List<String> dependsOn,
        OrchestrationStepStatus status) {

    public OrchestrationStepResult {
        dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
    }
}
