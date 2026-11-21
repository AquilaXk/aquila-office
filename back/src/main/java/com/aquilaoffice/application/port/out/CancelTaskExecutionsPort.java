package com.aquilaoffice.application.port.out;

import java.util.List;

/**
 * 특정 Task의 활성(QUEUED/RUNNING) 실행을 취소 상태로 일괄 전이하는 outbound port다.
 */
public interface CancelTaskExecutionsPort {

    List<SavedTaskExecution> cancelActiveByWorkspaceIdAndTaskId(
            Long workspaceId,
            Long taskId,
            String failureReason);
}
