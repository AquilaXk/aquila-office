package com.aquilaoffice.application.port.in;

import java.util.List;
import java.util.Objects;

public record CreateOrchestrationSessionCommand(
        Long workspaceId,
        Long sourceTaskId,
        String requesterGoogleSub,
        List<OrchestrationStepCommand> steps) {

    public CreateOrchestrationSessionCommand {
        if (steps == null) {
            steps = List.of();
        } else {
            if (steps.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("orchestration step must not be null");
            }
            steps = List.copyOf(steps);
        }
    }
}
