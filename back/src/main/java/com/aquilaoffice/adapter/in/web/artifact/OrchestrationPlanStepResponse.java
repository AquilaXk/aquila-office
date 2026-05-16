package com.aquilaoffice.adapter.in.web.artifact;

import com.aquilaoffice.application.port.in.OrchestrationPlanStepResult;
import java.util.List;

public record OrchestrationPlanStepResponse(
        Long stepId,
        String stepKey,
        int sequenceNo,
        String title,
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
