package com.aquilaoffice.application.port.out;

import java.time.LocalDateTime;

/**
 * 기준 시각을 초과한 고착 TaskExecution을 FAILED로 복구하는 outbound port다.
 */
public interface RecoverStaleExecutionsPort {

    int recoverStaleExecutions(
            Long workspaceId,
            LocalDateTime cutoffUtc,
            LocalDateTime nowUtc,
            String failureReason);
}
