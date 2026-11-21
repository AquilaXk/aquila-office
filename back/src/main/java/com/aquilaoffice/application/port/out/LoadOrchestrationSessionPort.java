package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.orchestration.OrchestrationSession;
import java.util.List;
import java.util.Optional;

public interface LoadOrchestrationSessionPort {

    Optional<OrchestrationSession> findByWorkspaceIdAndSessionId(Long workspaceId, Long sessionId);

    default List<SavedOrchestrationSession> findByWorkspaceIdOrderByIdDesc(Long workspaceId) {
        return List.of();
    }

    default Optional<SavedOrchestrationSession> findDetailByWorkspaceIdAndSessionId(Long workspaceId, Long sessionId) {
        return findByWorkspaceIdAndSessionId(workspaceId, sessionId)
                .map(session -> new SavedOrchestrationSession(
                        session.getId(),
                        session.getWorkspaceId(),
                        session.getSourceTaskId(),
                        session.getCreatedByGoogleSub(),
                        session.getStatus(),
                        session.getSteps().stream()
                                .map(step -> new SavedOrchestrationStep(
                                        step.getId(),
                                        step.getStepKey(),
                                        step.getInstruction(),
                                        step.getAgentId(),
                                        step.getTaskId(),
                                        step.getDependsOn(),
                                        step.getStatus()))
                                .toList(),
                        null,
                        null));
    }
}
