package com.aquilaoffice.application.port.out;

/**
 * GitHub 자격 증명 중복 여부를 조회하는 outbound port다.
 */
public interface ExistsGithubCredentialPort {

    boolean existsActiveDisplayName(Long workspaceId, String displayName);
}
