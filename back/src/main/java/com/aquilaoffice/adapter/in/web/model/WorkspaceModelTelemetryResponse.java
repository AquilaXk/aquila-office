package com.aquilaoffice.adapter.in.web.model;

import com.aquilaoffice.application.port.out.WorkspaceModelTelemetryEntry;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 워크스페이스 모델별 실행 통계 HTTP 응답 모델이다.
 */
@Schema(description = "워크스페이스 모델별 실행 통계")
public record WorkspaceModelTelemetryResponse(
        String modelName,
        long executionCount,
        long successCount,
        long failedCount,
        long totalPromptTokens,
        long totalCompletionTokens,
        long totalTokens,
        @Schema(nullable = true) Double avgDurationMs) {

    public static WorkspaceModelTelemetryResponse from(WorkspaceModelTelemetryEntry entry) {
        return new WorkspaceModelTelemetryResponse(
                entry.modelName(),
                entry.executionCount(),
                entry.successCount(),
                entry.failedCount(),
                entry.totalPromptTokens(),
                entry.totalCompletionTokens(),
                entry.totalTokens(),
                entry.avgDurationMs());
    }
}
