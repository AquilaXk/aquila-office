package com.aquilaoffice.adapter.out.persistence.task;

/**
 * 대시보드 요약에 필요한 Task 상태별 집계를 단일 쿼리로 반환하는 프로젝션이다.
 */
public record TaskDashboardCounts(
        long totalCount,
        long runningCount,
        long waitingApprovalCount,
        long completedCount,
        long failedCount) {

    public TaskDashboardCounts(
            long totalCount,
            long runningCount,
            long completedCount,
            long failedCount) {
        this(totalCount, runningCount, 0L, completedCount, failedCount);
    }
}
