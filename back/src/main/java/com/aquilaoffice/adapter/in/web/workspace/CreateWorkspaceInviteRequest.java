package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.CreateWorkspaceInviteCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 워크스페이스 초대 생성 HTTP 요청 body를 검증하고 command로 변환하는 DTO다.
 */
public record CreateWorkspaceInviteRequest(
        @NotBlank @Email @Size(max = 255) String email) {

    CreateWorkspaceInviteCommand toCommand(Long workspaceId, String requesterGoogleSub) {
        // email 정규화와 초대 규칙은 application/domain 계층에서 처리하고, web adapter는 요청 형태만 옮긴다.
        return new CreateWorkspaceInviteCommand(workspaceId, requesterGoogleSub, email);
    }
}
