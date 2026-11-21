package com.aquilaoffice.application.port.out;

import java.util.List;
import java.util.Optional;

public interface LoadWorkspaceAgentPort {

    List<SavedWorkspaceAgent> findActiveByWorkspaceId(Long workspaceId);

    Optional<SavedWorkspaceAgent> findActiveByWorkspaceIdAndAgentId(Long workspaceId, Long agentId);
}
