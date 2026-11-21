package com.aquilaoffice.adapter.in.web.orchestration;

import com.aquilaoffice.application.port.in.OrchestrationStepCommand;
import com.aquilaoffice.domain.orchestration.OrchestrationStep;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Multi-Agent 실행 계획 내 단일 step 요청 모델이다.
 */
@Schema(description = "Orchestration step 요청")
public record OrchestrationStepRequest(
        @Schema(description = "step 식별 키", maxLength = OrchestrationStep.MAX_STEP_KEY_LENGTH)
        @NotBlank
        @Size(max = OrchestrationStep.MAX_STEP_KEY_LENGTH)
        @Pattern(regexp = OrchestrationStep.STEP_KEY_REGEX)
        String stepKey,
        @Schema(description = "에이전트 실행 지시문", maxLength = OrchestrationStep.MAX_INSTRUCTION_LENGTH)
        @NotBlank
        @Size(max = OrchestrationStep.MAX_INSTRUCTION_LENGTH)
        String instruction,
        @Schema(description = "할당 에이전트 ID")
        @NotNull
        @Positive
        Long agentId,
        @Schema(description = "선행 step 키 목록")
        List<@NotBlank @Size(max = OrchestrationStep.MAX_STEP_KEY_LENGTH)
                @Pattern(regexp = OrchestrationStep.STEP_KEY_REGEX) String> dependsOn) {

    public OrchestrationStepCommand toCommand() {
        return new OrchestrationStepCommand(stepKey, instruction, agentId, dependsOn);
    }
}
