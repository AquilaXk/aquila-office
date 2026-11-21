package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.task.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;

public record TaskReportResult(
        Long reportId,
        Long taskId,
        TaskStatus status,
        String summary,
        String detail,
        String recommendedAction,
        List<TaskReportArtifactResult> artifacts,
        LocalDateTime createdAt) {

    public TaskReportResult {
        artifacts = List.copyOf(artifacts);
    }
}
