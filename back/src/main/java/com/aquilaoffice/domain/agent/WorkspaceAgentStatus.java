package com.aquilaoffice.domain.agent;

/**
 * Agent 생성과 외부 실행 환경 동기화 상태를 표현한다.
 */
public enum WorkspaceAgentStatus {
    CREATING,
    READY,
    SYNC_FAILED,
    ERROR,
    DISABLED
}
