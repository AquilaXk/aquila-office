package com.aquilaoffice.adapter.in.web.task;

import com.aquilaoffice.application.port.in.ReviewTaskApprovalCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * WAITING_USER Task 승인·반려 HTTP 요청 모델이다.
 */
@Schema(description = "Task 승인·반려 요청 본문")
public record ReviewTaskApprovalRequest(
        @Schema(description = "검토 결정 (APPROVE 또는 REJECT)", example = "APPROVE")
        @NotBlank
        @Pattern(regexp = "(?i)^(APPROVE|REJECT)$")
        String decision,
        @Schema(description = "승인·반려 코멘트", maxLength = ReviewTaskApprovalCommand.MAX_COMMENT_LENGTH, nullable = true)
        @Size(max = ReviewTaskApprovalCommand.MAX_COMMENT_LENGTH)
        String comment,
        @Schema(description = "APPROVE 시 즉시 재실행 여부", defaultValue = "false")
        Boolean runAfterApprove) {

    public ReviewTaskApprovalCommand toCommand(Long workspaceId, Long taskId, String requesterGoogleSub) {
        return new ReviewTaskApprovalCommand(
                workspaceId,
                taskId,
                requesterGoogleSub,
                decision,
                comment,
                Boolean.TRUE.equals(runAfterApprove));
    }
}
