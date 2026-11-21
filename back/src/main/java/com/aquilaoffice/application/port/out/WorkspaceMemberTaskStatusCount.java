package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.task.TaskStatus;

/**
 * 워크스페이스 내 담당 에이전트 소유자·상태별 작업 건수 집계 결과다.
 */
public record WorkspaceMemberTaskStatusCount(
        String agentOwnerGoogleSub,
        TaskStatus status,
        long count) {}
