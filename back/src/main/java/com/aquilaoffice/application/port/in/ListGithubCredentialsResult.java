package com.aquilaoffice.application.port.in;

/**
 * GitHub 자격 증명 목록 항목을 inbound adapter로 돌려주는 DTO다.
 */
public record ListGithubCredentialsResult(
        Long credentialId,
        Long workspaceId,
        String displayName,
        String maskedToken) {
}
