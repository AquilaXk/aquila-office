package com.aquilaoffice.application.port.out;

/**
 * Task 실행 lifecycle log를 저장할 때 필요한 command다.
 */
public record TaskExecutionLogCommand(
        Long workspaceId,
        Long taskId,
        Long executionId,
        Long agentId,
        String level,
        String message) {}
