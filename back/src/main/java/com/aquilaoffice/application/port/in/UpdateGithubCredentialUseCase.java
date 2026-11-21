package com.aquilaoffice.application.port.in;

/**
 * GitHub 자격 증명 수정 inbound port다.
 */
public interface UpdateGithubCredentialUseCase {

    UpdateGithubCredentialResult updateGithubCredential(UpdateGithubCredentialCommand command);
}
