package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CreateOrchestrationSessionResult;
import com.aquilaoffice.application.port.in.DispatchOrchestrationSessionResult;
import com.aquilaoffice.application.port.in.GetOrchestrationSessionResult;
import com.aquilaoffice.application.port.in.ListOrchestrationSessionsResult;
import com.aquilaoffice.application.port.in.OrchestrationStepResult;
import com.aquilaoffice.application.port.out.SavedOrchestrationSession;
import com.aquilaoffice.application.port.out.SavedOrchestrationStep;
import com.aquilaoffice.domain.orchestration.OrchestrationSession;
import com.aquilaoffice.domain.orchestration.OrchestrationStep;

final class OrchestrationResultMapper {

    private OrchestrationResultMapper() {}

    static CreateOrchestrationSessionResult toCreateResult(SavedOrchestrationSession session) {
        return new CreateOrchestrationSessionResult(
                session.sessionId(),
                session.workspaceId(),
                session.sourceTaskId(),
                session.status(),
                session.createdAt(),
                session.updatedAt(),
                session.steps().stream().map(OrchestrationResultMapper::toStepResult).toList());
    }

    static ListOrchestrationSessionsResult toListResult(SavedOrchestrationSession session) {
        return new ListOrchestrationSessionsResult(
                session.sessionId(),
                session.workspaceId(),
                session.sourceTaskId(),
                session.status(),
                session.createdAt(),
                session.updatedAt(),
                session.steps().stream().map(OrchestrationResultMapper::toStepResult).toList());
    }

    static GetOrchestrationSessionResult toGetResult(SavedOrchestrationSession session) {
        return new GetOrchestrationSessionResult(
                session.sessionId(),
                session.workspaceId(),
                session.sourceTaskId(),
                session.status(),
                session.createdAt(),
                session.updatedAt(),
                session.steps().stream().map(OrchestrationResultMapper::toStepResult).toList());
    }

    static DispatchOrchestrationSessionResult toDispatchResult(
            OrchestrationSession session,
            int dispatchedCount) {
        return new DispatchOrchestrationSessionResult(
                session.getId(),
                session.getWorkspaceId(),
                session.getSourceTaskId(),
                session.getStatus(),
                dispatchedCount,
                session.getSteps().stream().map(OrchestrationResultMapper::toStepResult).toList());
    }

    private static OrchestrationStepResult toStepResult(SavedOrchestrationStep step) {
        return new OrchestrationStepResult(
                step.stepKey(),
                step.instruction(),
                step.agentId(),
                step.taskId(),
                step.dependsOn(),
                step.status());
    }

    private static OrchestrationStepResult toStepResult(OrchestrationStep step) {
        return new OrchestrationStepResult(
                step.getStepKey(),
                step.getInstruction(),
                step.getAgentId(),
                step.getTaskId(),
                step.getDependsOn(),
                step.getStatus());
    }
}
