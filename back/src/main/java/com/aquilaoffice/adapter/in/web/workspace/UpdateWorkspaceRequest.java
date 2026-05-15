package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.UpdateWorkspaceCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 워크스페이스 정보 수정 HTTP 요청 body를 검증하고 command로 변환하는 DTO다.
 */
public record UpdateWorkspaceRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description) {

    UpdateWorkspaceCommand toCommand(Long workspaceId, String requesterGoogleSub) {
        return new UpdateWorkspaceCommand(workspaceId, requesterGoogleSub, name, description);
    }
}
