package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.orchestration.OrchestrationSessionStatus;
import java.util.List;

public record CreateOrchestrationSessionResult(
        Long sessionId,
        Long workspaceId,
        Long sourceTaskId,
        OrchestrationSessionStatus status,
        List<OrchestrationStepResult> steps) {

    public CreateOrchestrationSessionResult {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
