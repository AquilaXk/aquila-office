package com.aquilaoffice.application.port.in;

/**
 * GitHub 자격 증명 등록 inbound port다.
 */
public interface CreateGithubCredentialUseCase {

    CreateGithubCredentialResult createGithubCredential(CreateGithubCredentialCommand command);
}
