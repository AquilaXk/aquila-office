package com.aquilaoffice.adapter.out.persistence.agent;

/**
 * 대시보드 요약에 필요한 Agent 상태별 집계를 단일 쿼리로 반환하는 프로젝션이다.
 */
public record WorkspaceAgentDashboardCounts(long activeCount, long readyCount, long errorCount) {}
