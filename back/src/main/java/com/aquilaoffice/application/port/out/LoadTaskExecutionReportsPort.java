package com.aquilaoffice.application.port.out;

import java.util.List;

public interface LoadTaskExecutionReportsPort {

    List<SavedTaskExecution> findByWorkspaceIdAndTaskIdOrderByCreatedAtDesc(Long workspaceId, Long taskId);
}
