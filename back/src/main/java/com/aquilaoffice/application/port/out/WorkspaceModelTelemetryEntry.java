package com.aquilaoffice.application.port.out;

/**
 * 워크스페이스 모델별 실행 통계 집계 항목이다.
 */
public record WorkspaceModelTelemetryEntry(
        String modelName,
        long executionCount,
        long successCount,
        long failedCount,
        long totalPromptTokens,
        long totalCompletionTokens,
        Double avgDurationMs) {

    public long totalTokens() {
        return Math.max(0L, totalPromptTokens) + Math.max(0L, totalCompletionTokens);
    }
}
