package com.aquilaoffice.adapter.in.web.task;

import com.aquilaoffice.application.port.in.TaskReportResult;
import com.aquilaoffice.domain.task.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;

public record TaskReportResponse(
        Long reportId,
        Long taskId,
        TaskStatus status,
        String summary,
        String detail,
        String recommendedAction,
        List<TaskReportArtifactResponse> artifacts,
        LocalDateTime createdAt) {

    static TaskReportResponse from(TaskReportResult result) {
        return new TaskReportResponse(
                result.reportId(),
                result.taskId(),
                result.status(),
                result.summary(),
                result.detail(),
                result.recommendedAction(),
                result.artifacts().stream().map(TaskReportArtifactResponse::from).toList(),
                result.createdAt());
    }
}
