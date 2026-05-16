package com.aquilaoffice.application.port.out;

import java.util.Optional;

public interface LoadTaskPort {

    Optional<TaskDetail> findByWorkspaceIdAndTaskId(Long workspaceId, Long taskId);
}
