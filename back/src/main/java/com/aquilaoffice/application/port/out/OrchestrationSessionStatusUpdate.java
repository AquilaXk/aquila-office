package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.orchestration.OrchestrationSessionStatus;

public record OrchestrationSessionStatusUpdate(
        Long workspaceId,
        Long sessionId,
        OrchestrationSessionStatus status) {}
