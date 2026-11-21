package com.aquilaoffice.adapter.in.web.orchestration;

import com.aquilaoffice.application.port.in.DispatchOrchestrationSessionResult;
import com.aquilaoffice.domain.orchestration.OrchestrationSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Multi-Agent 실행 계획 디스패치 HTTP 응답 모델이다.
 */
@Schema(description = "Orchestration session 디스패치 응답")
public record DispatchOrchestrationSessionResponse(
        @Schema(description = "세션 ID")
        Long sessionId,
        @Schema(description = "워크스페이스 ID")
        Long workspaceId,
        @Schema(description = "원본 Task ID")
        Long sourceTaskId,
        @Schema(description = "세션 상태")
        OrchestrationSessionStatus status,
        @Schema(description = "이번 호출로 실행된 step 수")
        int dispatchedCount,
        @Schema(description = "실행 step 목록")
        List<OrchestrationStepResponse> steps) {

    public static DispatchOrchestrationSessionResponse from(DispatchOrchestrationSessionResult result) {
        return new DispatchOrchestrationSessionResponse(
                result.sessionId(),
                result.workspaceId(),
                result.sourceTaskId(),
                result.status(),
                result.dispatchedCount(),
                result.steps().stream().map(OrchestrationStepResponse::from).toList());
    }
}
