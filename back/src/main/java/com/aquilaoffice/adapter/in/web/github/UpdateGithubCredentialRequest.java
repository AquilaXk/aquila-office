package com.aquilaoffice.adapter.in.web.github;

import com.aquilaoffice.application.port.in.UpdateGithubCredentialCommand;
import jakarta.validation.constraints.Size;

/**
 * GitHub 자격 증명 부분 수정 HTTP 요청 body를 command로 변환하는 DTO다.
 */
public record UpdateGithubCredentialRequest(
        @Size(max = 120) String displayName,
        String token) {

    UpdateGithubCredentialCommand toCommand(Long workspaceId, Long credentialId, String requesterGoogleSub) {
        return new UpdateGithubCredentialCommand(workspaceId, credentialId, requesterGoogleSub, displayName, token);
    }
}
