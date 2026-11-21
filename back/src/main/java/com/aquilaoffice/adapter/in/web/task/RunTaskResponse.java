package com.aquilaoffice.adapter.in.web.task;

import com.aquilaoffice.application.port.in.RunTaskResult;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.task.TaskStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Task 실행 HTTP 응답 모델이다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RunTaskResponse(
        Long workspaceId,
        Long taskId,
        Long executionId,
        Long agentId,
        TaskStatus taskStatus,
        TaskExecutionStatus executionStatus,
        String finalText,
        String failureReason) {

    public static RunTaskResponse from(RunTaskResult result) {
        return new RunTaskResponse(
                result.workspaceId(),
                result.taskId(),
                result.executionId(),
                result.agentId(),
                result.taskStatus(),
                result.executionStatus(),
                result.finalText(),
                result.failureReason());
    }
}
