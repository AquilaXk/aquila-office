package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.application.port.in.DeleteSlackIntegrationResult;

/**
 * Slack 수동 연동 삭제 HTTP 응답 모델이다.
 */
public record DeleteSlackIntegrationResponse(
        Long id,
        Long workspaceId,
        boolean active) {

    public static DeleteSlackIntegrationResponse from(DeleteSlackIntegrationResult result) {
        return new DeleteSlackIntegrationResponse(
                result.id(),
                result.workspaceId(),
                result.active());
    }
}
