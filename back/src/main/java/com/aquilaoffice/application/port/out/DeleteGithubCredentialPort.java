package com.aquilaoffice.application.port.out;

/**
 * GitHub 자격 증명 삭제를 persistence adapter에 위임하는 outbound port다.
 */
public interface DeleteGithubCredentialPort {

    void delete(Long credentialId, Long workspaceId);
}
