package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.orchestration.OrchestrationSessionStatus;
import java.util.List;

public record DispatchOrchestrationSessionResult(
        Long sessionId,
        Long workspaceId,
        Long sourceTaskId,
        OrchestrationSessionStatus status,
        int dispatchedCount,
        List<OrchestrationStepResult> steps) {

    public DispatchOrchestrationSessionResult {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
