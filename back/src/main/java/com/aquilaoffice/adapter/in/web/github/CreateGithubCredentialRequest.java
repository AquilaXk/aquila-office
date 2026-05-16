package com.aquilaoffice.adapter.in.web.github;

import com.aquilaoffice.application.port.in.CreateGithubCredentialCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * GitHub 자격 증명 등록 HTTP 요청 body를 검증하고 command로 변환하는 DTO다.
 */
public record CreateGithubCredentialRequest(
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank String token) {

    CreateGithubCredentialCommand toCommand(Long workspaceId, String requesterGoogleSub) {
        return new CreateGithubCredentialCommand(workspaceId, requesterGoogleSub, displayName, token);
    }
}
