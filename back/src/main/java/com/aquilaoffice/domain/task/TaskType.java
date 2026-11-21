package com.aquilaoffice.domain.task;

/**
 * Agent에게 요청할 작업 유형이다.
 */
public enum TaskType {
    CODE_REVIEW,
    PR_REVIEW,
    BUG_FIX,
    FEATURE_IMPLEMENTATION,
    REFACTORING,
    TEST_CREATION,
    DOCUMENTATION,
    PR_CREATION,
    OTHER
}
