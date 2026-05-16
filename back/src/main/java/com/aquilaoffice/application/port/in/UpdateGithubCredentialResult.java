package com.aquilaoffice.application.port.in;

/**
 * GitHub 자격 증명 수정 결과를 inbound adapter로 돌려주는 DTO다.
 */
public record UpdateGithubCredentialResult(
        Long credentialId,
        Long workspaceId,
        String displayName,
        String maskedToken) {
}
