package com.aquilaoffice.adapter.in.web.orchestration;

import com.aquilaoffice.application.port.in.CreateOrchestrationSessionCommand;
import com.aquilaoffice.domain.orchestration.OrchestrationSession;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Multi-Agent 실행 계획 세션 생성 HTTP 요청 모델이다.
 */
@Schema(description = "Orchestration session 생성 요청")
public record CreateOrchestrationSessionRequest(
        @Schema(description = "원본 Task ID")
        @NotNull
        @Positive
        Long sourceTaskId,
        @Schema(description = "실행 step 목록 (최대 20개)")
        @NotEmpty
        @Size(max = OrchestrationSession.MAX_STEPS)
        List<@NotNull @Valid OrchestrationStepRequest> steps) {

    public CreateOrchestrationSessionCommand toCommand(Long workspaceId, String requesterGoogleSub) {
        List<OrchestrationStepRequest> safeSteps = steps == null ? List.of() : steps;
        return new CreateOrchestrationSessionCommand(
                workspaceId,
                sourceTaskId,
                requesterGoogleSub,
                safeSteps.stream().map(OrchestrationStepRequest::toCommand).toList());
    }
}
