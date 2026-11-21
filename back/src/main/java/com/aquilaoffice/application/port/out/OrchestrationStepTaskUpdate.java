package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.orchestration.OrchestrationStepStatus;

public record OrchestrationStepTaskUpdate(
        Long workspaceId,
        Long sessionId,
        String stepKey,
        Long taskId,
        OrchestrationStepStatus status) {}
