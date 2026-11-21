package com.aquilaoffice.adapter.in.web.orchestration;

import com.aquilaoffice.application.port.in.DispatchOrchestrationSessionCommand;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Multi-Agent 실행 계획 디스패치 HTTP 요청 모델이다.
 */
@Schema(description = "Orchestration session 디스패치 요청")
public record DispatchOrchestrationSessionRequest(
        @Schema(description = "실행 완료 후 PR 생성 여부", defaultValue = "false", nullable = true)
        Boolean createPr) {

    public DispatchOrchestrationSessionCommand toCommand(
            Long workspaceId,
            Long sessionId,
            String requesterGoogleSub) {
        return new DispatchOrchestrationSessionCommand(
                workspaceId,
                sessionId,
                requesterGoogleSub,
                Boolean.TRUE.equals(createPr));
    }
}
