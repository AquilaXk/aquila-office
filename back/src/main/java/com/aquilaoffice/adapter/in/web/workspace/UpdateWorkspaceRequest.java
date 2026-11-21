package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.UpdateWorkspaceCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 워크스페이스 정보 수정 HTTP 요청 body를 검증하고 command로 변환하는 DTO다.
 */
@Schema(description = "워크스페이스 정보 수정 요청")
public record UpdateWorkspaceRequest(
        @Schema(description = "워크스페이스 이름", example = "Backend Team")
        @NotBlank
        @Size(max = 100)
        String name,
        @Schema(description = "워크스페이스 설명", nullable = true)
        @Size(max = 500)
        String description,
        @Schema(description = "일일 토큰 예산 상한 (null 또는 0 이하는 무제한)", nullable = true, example = "100000")
        Integer dailyTokenLimit) {

    public UpdateWorkspaceRequest(String name, String description) {
        this(name, description, null);
    }

    UpdateWorkspaceCommand toCommand(Long workspaceId, String requesterGoogleSub) {
        return new UpdateWorkspaceCommand(workspaceId, requesterGoogleSub, name, description, dailyTokenLimit);
    }
}
