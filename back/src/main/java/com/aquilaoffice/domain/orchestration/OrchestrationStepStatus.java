package com.aquilaoffice.domain.orchestration;

public enum OrchestrationStepStatus {
    PENDING,
    DISPATCHED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    SKIPPED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == SKIPPED;
    }
}
