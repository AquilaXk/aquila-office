package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 멤버별 작업 통계와 점수 순위를 adapter와 독립된 application 값으로 표현한다.
 */
public record ListWorkspaceMemberTaskStatsResult(
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
        int totalScore) {}
