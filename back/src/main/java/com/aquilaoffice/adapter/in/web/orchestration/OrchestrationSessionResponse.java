package com.aquilaoffice.adapter.in.web.orchestration;

import com.aquilaoffice.application.port.in.CreateOrchestrationSessionResult;
import com.aquilaoffice.application.port.in.GetOrchestrationSessionResult;
import com.aquilaoffice.application.port.in.ListOrchestrationSessionsResult;
import com.aquilaoffice.domain.orchestration.OrchestrationSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Multi-Agent 실행 계획 세션 HTTP 응답 모델이다.
 */
@Schema(description = "Orchestration session 응답")
public record OrchestrationSessionResponse(
        @Schema(description = "세션 ID")
        Long sessionId,
        @Schema(description = "워크스페이스 ID")
        Long workspaceId,
        @Schema(description = "원본 Task ID")
        Long sourceTaskId,
        @Schema(description = "세션 상태")
        OrchestrationSessionStatus status,
        @Schema(description = "생성 시각", nullable = true)
        LocalDateTime createdAt,
        @Schema(description = "수정 시각", nullable = true)
        LocalDateTime updatedAt,
        @Schema(description = "실행 step 목록")
        List<OrchestrationStepResponse> steps) {

    public static OrchestrationSessionResponse from(CreateOrchestrationSessionResult result) {
        return new OrchestrationSessionResponse(
                result.sessionId(),
                result.workspaceId(),
                result.sourceTaskId(),
                result.status(),
                result.createdAt(),
                result.updatedAt(),
                result.steps().stream().map(OrchestrationStepResponse::from).toList());
    }

    public static OrchestrationSessionResponse from(ListOrchestrationSessionsResult result) {
        return new OrchestrationSessionResponse(
                result.sessionId(),
                result.workspaceId(),
                result.sourceTaskId(),
                result.status(),
                result.createdAt(),
                result.updatedAt(),
                result.steps().stream().map(OrchestrationStepResponse::from).toList());
    }

    public static OrchestrationSessionResponse from(GetOrchestrationSessionResult result) {
        return new OrchestrationSessionResponse(
                result.sessionId(),
                result.workspaceId(),
                result.sourceTaskId(),
                result.status(),
                result.createdAt(),
                result.updatedAt(),
                result.steps().stream().map(OrchestrationStepResponse::from).toList());
    }
}
