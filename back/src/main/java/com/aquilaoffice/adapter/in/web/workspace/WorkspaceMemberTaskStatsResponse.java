package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.ListWorkspaceMemberTaskStatsResult;

/**
 * 워크스페이스 멤버별 작업 통계 API 응답 DTO다.
 */
public record WorkspaceMemberTaskStatsResponse(
        Long memberId,
        String memberName,
        int rank,
        int taskCount,
        int completedTaskCount,
        int runningTaskCount,
        int failedTaskCount,
        int waitingUserTaskCount,
        int healthScore,
        int flowScore,
        int impactScore,
        int totalScore) {

    static WorkspaceMemberTaskStatsResponse from(ListWorkspaceMemberTaskStatsResult result) {
        return new WorkspaceMemberTaskStatsResponse(
                result.memberId(),
                result.memberName(),
                result.rank(),
                result.taskCount(),
                result.completedTaskCount(),
                result.runningTaskCount(),
                result.failedTaskCount(),
                result.waitingUserTaskCount(),
                result.healthScore(),
                result.flowScore(),
                result.impactScore(),
                result.totalScore());
    }
}
