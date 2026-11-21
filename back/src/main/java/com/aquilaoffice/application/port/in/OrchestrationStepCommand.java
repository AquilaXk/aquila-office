package com.aquilaoffice.application.port.in;

import java.util.List;

public record OrchestrationStepCommand(
        String stepKey,
        String instruction,
        Long agentId,
        List<String> dependsOn) {

    public OrchestrationStepCommand {
        dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
    }
}
