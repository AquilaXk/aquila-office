package com.aquilaoffice.adapter.in.web.task;

import com.aquilaoffice.application.port.in.RecoverStaleTasksResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 고착 실행·Task 복구 HTTP 응답 모델이다.
 */
@Schema(description = "고착 실행·Task 복구 응답")
public record RecoverStaleTasksResponse(
        @Schema(description = "복구된 Task 수")
        int recoveredTaskCount,
        @Schema(description = "복구된 실행 수")
        int recoveredExecutionCount) {

    public static RecoverStaleTasksResponse from(RecoverStaleTasksResult result) {
        return new RecoverStaleTasksResponse(result.recoveredTaskCount(), result.recoveredExecutionCount());
    }
}
