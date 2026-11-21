package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.orchestration.OrchestrationSessionStatus;
import java.time.LocalDateTime;
import java.util.List;

public record CreateOrchestrationSessionResult(
        Long sessionId,
        Long workspaceId,
        Long sourceTaskId,
        OrchestrationSessionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrchestrationStepResult> steps) {

    public CreateOrchestrationSessionResult(
            Long sessionId,
            Long workspaceId,
            Long sourceTaskId,
            OrchestrationSessionStatus status,
            List<OrchestrationStepResult> steps) {
        this(sessionId, workspaceId, sourceTaskId, status, null, null, steps);
    }

    public CreateOrchestrationSessionResult {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
