package com.aquilaoffice.adapter.in.web.orchestration;

import com.aquilaoffice.application.port.in.OrchestrationStepResult;
import com.aquilaoffice.domain.orchestration.OrchestrationStepStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Multi-Agent 실행 계획 step HTTP 응답 모델이다.
 */
@Schema(description = "Orchestration step 응답")
public record OrchestrationStepResponse(
        @Schema(description = "step 식별 키")
        String stepKey,
        @Schema(description = "에이전트 실행 지시문")
        String instruction,
        @Schema(description = "할당 에이전트 ID")
        Long agentId,
        @Schema(description = "생성된 하위 Task ID", nullable = true)
        Long taskId,
        @Schema(description = "선행 step 키 목록")
        List<String> dependsOn,
        @Schema(description = "step 상태")
        OrchestrationStepStatus status) {

    public static OrchestrationStepResponse from(OrchestrationStepResult result) {
        return new OrchestrationStepResponse(
                result.stepKey(),
                result.instruction(),
                result.agentId(),
                result.taskId(),
                result.dependsOn(),
                result.status());
    }
}
