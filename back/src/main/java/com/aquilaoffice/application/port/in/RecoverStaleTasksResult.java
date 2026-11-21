package com.aquilaoffice.application.port.in;

/**
 * 고착 복구로 FAILED 처리된 Task와 Execution 수량이다.
 */
public record RecoverStaleTasksResult(int recoveredTaskCount, int recoveredExecutionCount) {}
