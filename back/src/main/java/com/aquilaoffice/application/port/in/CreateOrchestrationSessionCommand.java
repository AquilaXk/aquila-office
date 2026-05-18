package com.aquilaoffice.application.port.in;

import java.util.List;

public record CreateOrchestrationSessionCommand(
        Long workspaceId,
        Long sourceTaskId,
        String requesterGoogleSub,
        List<OrchestrationStepCommand> steps) {

    public CreateOrchestrationSessionCommand {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
