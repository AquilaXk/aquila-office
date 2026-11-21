package com.aquilaoffice.application.port.out;

import java.util.List;

public interface LoadTaskExecutionLogsPort {

    List<SavedTaskExecutionLog> findByWorkspaceIdAndTaskIdOrderByCreatedAtAsc(Long workspaceId, Long taskId);
}
