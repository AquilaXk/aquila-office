package com.aquilaoffice.application.port.out;

import java.util.List;
import java.util.Optional;

public interface LoadTasksPort {

    List<TaskSummary> findByWorkspaceId(Long workspaceId);

    default Optional<List<WorkspaceMemberTaskStatusCount>> countGroupedByAgentOwnerAndStatus(Long workspaceId) {
        return Optional.empty();
    }
}
