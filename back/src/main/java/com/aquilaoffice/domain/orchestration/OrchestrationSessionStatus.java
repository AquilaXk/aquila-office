package com.aquilaoffice.domain.orchestration;

public enum OrchestrationSessionStatus {
    PLANNED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELED;
    }
}
