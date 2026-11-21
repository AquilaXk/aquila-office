package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.CreateWorkspaceCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 워크스페이스 생성 HTTP 요청 body를 검증하고 command로 변환하는 DTO다.
 */
public record CreateWorkspaceRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description) {

    CreateWorkspaceCommand toCommand(String creatorGoogleSub) {
        // name/description 정규화는 domain 계층에서 처리하고, web adapter는 요청 형태만 command로 옮긴다.
        return new CreateWorkspaceCommand(name, description, creatorGoogleSub);
    }
}
