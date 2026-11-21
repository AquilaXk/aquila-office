package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.domain.workspace.WorkspacePresencePosition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 워크스페이스 presence 위치 HTTP 요청 body의 position 값을 검증하는 DTO다.
 */
public record WorkspacePresencePositionRequest(
        @NotBlank @Size(max = 30) String left,
        @NotBlank @Size(max = 30) String top) {

    WorkspacePresencePosition toDomain() {
        return new WorkspacePresencePosition(left, top);
    }
}
