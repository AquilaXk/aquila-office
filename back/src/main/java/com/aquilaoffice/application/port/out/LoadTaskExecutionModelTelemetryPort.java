package com.aquilaoffice.application.port.out;

import java.util.List;

/**
 * 워크스페이스의 모델별 실행 통계를 조회하는 outbound port다.
 */
public interface LoadTaskExecutionModelTelemetryPort {

    List<WorkspaceModelTelemetryEntry> loadModelTelemetry(Long workspaceId, int limit);
}
