package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.orchestration.OrchestrationSession;
import java.util.Optional;

public interface LoadOrchestrationSessionPort {

    Optional<OrchestrationSession> findByWorkspaceIdAndSessionId(Long workspaceId, Long sessionId);
}
