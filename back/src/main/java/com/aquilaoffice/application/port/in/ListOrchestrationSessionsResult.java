package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.orchestration.OrchestrationSessionStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 워크스페이스 Multi-Agent orchestration session 목록 조회 결과 항목이다.
 */
public record ListOrchestrationSessionsResult(
        Long sessionId,
        Long workspaceId,
        Long sourceTaskId,
        OrchestrationSessionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrchestrationStepResult> steps) {

    public ListOrchestrationSessionsResult {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
