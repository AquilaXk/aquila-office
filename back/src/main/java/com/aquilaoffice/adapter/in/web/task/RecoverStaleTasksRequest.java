package com.aquilaoffice.adapter.in.web.task;

import com.aquilaoffice.application.port.in.RecoverStaleTasksCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 고착 실행·Task 복구 HTTP 요청 모델이다.
 */
@Schema(description = "고착 실행·Task 복구 요청 본문")
public record RecoverStaleTasksRequest(
        @Schema(
                description = "고착 판정 기준(분)",
                minimum = "1",
                maximum = "1440",
                defaultValue = "15",
                nullable = true)
        @Min(RecoverStaleTasksCommand.MIN_STALE_MINUTES)
        @Max(RecoverStaleTasksCommand.MAX_STALE_MINUTES)
        Integer staleMinutes) {

    public RecoverStaleTasksCommand toCommand(Long workspaceId, String requesterGoogleSub) {
        return RecoverStaleTasksCommand.of(workspaceId, requesterGoogleSub, staleMinutes);
    }
}
