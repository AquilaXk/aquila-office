package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.UpdateWorkspacePresencePositionCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 워크스페이스 presence 위치 갱신 HTTP 요청 body를 command로 변환하는 DTO다.
 */
public record WorkspacePresencePositionUpdateRequest(
        @NotNull @Valid WorkspacePresencePositionRequest position,
        @Size(max = 30) String status) {

    UpdateWorkspacePresencePositionCommand toCommand(Long workspaceId, String memberGoogleSub) {
        return new UpdateWorkspacePresencePositionCommand(workspaceId, memberGoogleSub, position.toDomain(), status);
    }
}
