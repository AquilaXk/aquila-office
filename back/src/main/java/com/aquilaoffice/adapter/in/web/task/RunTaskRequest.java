package com.aquilaoffice.adapter.in.web.task;

import com.aquilaoffice.application.port.in.RunTaskCommand;

/**
 * Task 실행 HTTP 요청 모델이다.
 */
public record RunTaskRequest(Boolean createPr) {

    public RunTaskCommand toCommand(Long workspaceId, Long taskId, String requesterGoogleSub) {
        return new RunTaskCommand(workspaceId, taskId, requesterGoogleSub, Boolean.TRUE.equals(createPr));
    }
}
