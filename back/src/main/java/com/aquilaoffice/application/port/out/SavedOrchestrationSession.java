package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.orchestration.OrchestrationSessionStatus;
import java.time.LocalDateTime;
import java.util.List;

public record SavedOrchestrationSession(
        Long sessionId,
        Long workspaceId,
        Long sourceTaskId,
        String createdByGoogleSub,
        OrchestrationSessionStatus status,
        List<SavedOrchestrationStep> steps,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public SavedOrchestrationSession {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
