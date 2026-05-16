package com.aquilaoffice.application.port.in;

import java.util.List;

public record OrchestrationPlanStepResult(
        Long stepId,
        String stepKey,
        int sequenceNo,
        String title,
        List<OrchestrationPlanFileResult> files) {}
