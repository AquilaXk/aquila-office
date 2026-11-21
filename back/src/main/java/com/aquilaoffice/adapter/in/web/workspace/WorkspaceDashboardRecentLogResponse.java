package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.WorkspaceDashboardRecentLogResult;
import java.time.LocalDateTime;

public record WorkspaceDashboardRecentLogResponse(
        Long executionId,
        Long taskId,
        Long agentId,
        String level,
        String message,
        LocalDateTime createdAt) {

    static WorkspaceDashboardRecentLogResponse from(WorkspaceDashboardRecentLogResult result) {
        return new WorkspaceDashboardRecentLogResponse(
                result.executionId(),
                result.taskId(),
                result.agentId(),
                result.level(),
                result.message(),
                result.createdAt());
    }
}
