package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.orchestration.OrchestrationSessionStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 단건 Multi-Agent orchestration session 상세 조회 결과다.
 */
public record GetOrchestrationSessionResult(
        Long sessionId,
        Long workspaceId,
        Long sourceTaskId,
        OrchestrationSessionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrchestrationStepResult> steps) {

    public GetOrchestrationSessionResult {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
