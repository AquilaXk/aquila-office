package com.aquilaoffice.adapter.in.web.task;

import com.aquilaoffice.application.port.in.RequestTaskApprovalCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Task 사용자 승인 대기 전환 HTTP 요청 모델이다.
 */
@Schema(description = "Task 승인 요청 본문")
public record RequestTaskApprovalRequest(
        @Schema(description = "승인 요청 사유", maxLength = RequestTaskApprovalCommand.MAX_REASON_LENGTH, nullable = true)
        @Size(max = RequestTaskApprovalCommand.MAX_REASON_LENGTH)
        String reason) {

    public RequestTaskApprovalCommand toCommand(Long workspaceId, Long taskId, String requesterGoogleSub) {
        return new RequestTaskApprovalCommand(workspaceId, taskId, requesterGoogleSub, reason);
    }
}
