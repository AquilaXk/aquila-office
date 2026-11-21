package com.aquilaoffice.application.port.out;

import java.time.LocalDateTime;

/**
 * 워크스페이스의 지정 시각 이후 누적 토큰 사용량을 집계하는 outbound port다.
 */
public interface LoadTaskExecutionTokenUsagePort {

    long sumTokensSince(Long workspaceId, LocalDateTime sinceUtc);
}
