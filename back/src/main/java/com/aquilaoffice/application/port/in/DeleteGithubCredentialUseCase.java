package com.aquilaoffice.application.port.in;

/**
 * GitHub 자격 증명 삭제 inbound port다.
 */
public interface DeleteGithubCredentialUseCase {

    void deleteGithubCredential(DeleteGithubCredentialCommand command);
}
