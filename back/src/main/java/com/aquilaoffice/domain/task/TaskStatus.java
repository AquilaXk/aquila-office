package com.aquilaoffice.domain.task;

/**
 * Task가 업무 흐름에서 가질 수 있는 상태다.
 */
public enum TaskStatus {
    REQUESTED,
    ASSIGNED,
    IN_PROGRESS,
    WAITING_USER,
    COMPLETED,
    FAILED,
    CANCELED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELED;
    }
}
