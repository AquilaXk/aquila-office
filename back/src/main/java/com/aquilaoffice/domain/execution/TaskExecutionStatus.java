package com.aquilaoffice.domain.execution;

/**
 * Task 실행 기록의 처리 상태다.
 */
public enum TaskExecutionStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELED
}
