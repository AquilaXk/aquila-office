package com.aquilaoffice.adapter.in.web.task;

import com.aquilaoffice.application.port.in.TaskReportArtifactResult;

public record TaskReportArtifactResponse(
        Long artifactId,
        String artifactType,
        String name,
        String url) {

    static TaskReportArtifactResponse from(TaskReportArtifactResult result) {
        return new TaskReportArtifactResponse(
                result.artifactId(),
                result.artifactType(),
                result.name(),
                result.url());
    }
}
