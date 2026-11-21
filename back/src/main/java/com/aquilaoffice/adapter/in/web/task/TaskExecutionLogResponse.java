package com.aquilaoffice.adapter.in.web.task;

import com.aquilaoffice.application.port.in.TaskExecutionLogResult;
import java.time.LocalDateTime;

/**
 * Task 실행 log HTTP 응답 모델이다.
 */
public record TaskExecutionLogResponse(
        Long logId,
        Long workspaceId,
        Long taskId,
        Long taskExecutionId,
        Long agentId,
        String level,
        String message,
        LocalDateTime createdAt) {

    public static TaskExecutionLogResponse from(TaskExecutionLogResult result) {
        return new TaskExecutionLogResponse(
                result.logId(),
                result.workspaceId(),
                result.taskId(),
                result.taskExecutionId(),
                result.agentId(),
                result.level(),
                result.message(),
                result.createdAt());
    }
}
