package com.aquilaoffice.adapter.in.web.github;

import com.aquilaoffice.application.port.in.CreateGithubCredentialResult;
import com.aquilaoffice.application.port.in.ListGithubCredentialsResult;
import com.aquilaoffice.application.port.in.UpdateGithubCredentialResult;

/**
 * GitHub 자격 증명 API 응답 DTO다. token 원문은 포함하지 않고 마스킹된 값만 노출한다.
 */
public record GithubCredentialResponse(
        Long credentialId,
        Long workspaceId,
        String displayName,
        String maskedToken) {

    static GithubCredentialResponse from(CreateGithubCredentialResult result) {
        return new GithubCredentialResponse(
                result.credentialId(),
                result.workspaceId(),
                result.displayName(),
                result.maskedToken());
    }

    static GithubCredentialResponse from(ListGithubCredentialsResult result) {
        return new GithubCredentialResponse(
                result.credentialId(),
                result.workspaceId(),
                result.displayName(),
                result.maskedToken());
    }

    static GithubCredentialResponse from(UpdateGithubCredentialResult result) {
        return new GithubCredentialResponse(
                result.credentialId(),
                result.workspaceId(),
                result.displayName(),
                result.maskedToken());
    }
}
