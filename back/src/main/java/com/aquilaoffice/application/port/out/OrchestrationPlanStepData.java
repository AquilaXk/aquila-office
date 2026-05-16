package com.aquilaoffice.application.port.out;

import java.util.List;

public record OrchestrationPlanStepData(
        Long stepId,
        String stepKey,
        int sequenceNo,
        String title,
        List<OrchestrationPlanFileData> files) {}
