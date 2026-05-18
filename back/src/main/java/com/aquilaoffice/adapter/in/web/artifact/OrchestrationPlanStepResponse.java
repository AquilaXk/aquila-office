package com.aquilaoffice.adapter.in.web.artifact;

import com.aquilaoffice.application.port.in.OrchestrationPlanStepResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "오케스트레이션 plan step 산출물 응답")
public record OrchestrationPlanStepResponse(
        @Schema(description = "Step id", example = "11")
        Long stepId,
        @Schema(description = "Step key", example = "requirements")
        String stepKey,
        @Schema(description = "Step 표시 순서", example = "1")
        int sequenceNo,
        @Schema(description = "Step 제목", example = "요구사항 확인")
        String title,
        @Schema(description = "Step에서 생성한 파일 목록")
        List<OrchestrationPlanFileResponse> files) {

    static OrchestrationPlanStepResponse from(OrchestrationPlanStepResult result) {
        return new OrchestrationPlanStepResponse(
                result.stepId(),
                result.stepKey(),
                result.sequenceNo(),
                result.title(),
                result.files().stream().map(OrchestrationPlanFileResponse::from).toList());
    }
}
