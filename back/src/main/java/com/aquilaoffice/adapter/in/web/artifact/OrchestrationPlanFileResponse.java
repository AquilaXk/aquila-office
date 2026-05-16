package com.aquilaoffice.adapter.in.web.artifact;

import com.aquilaoffice.application.port.in.OrchestrationPlanFileResult;

public record OrchestrationPlanFileResponse(
        String path,
        String name,
        String contentType,
        Long sizeBytes,
        boolean exists) {

    static OrchestrationPlanFileResponse from(OrchestrationPlanFileResult result) {
        return new OrchestrationPlanFileResponse(
                result.path(),
                result.name(),
                result.contentType(),
                result.sizeBytes(),
                result.exists());
    }
}
