package com.aquilaoffice.application.port.out;

import java.util.List;

public interface LoadTasksPort {

    List<TaskSummary> findByWorkspaceId(Long workspaceId);
}
