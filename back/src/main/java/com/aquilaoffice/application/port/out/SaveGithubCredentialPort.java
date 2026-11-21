package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.github.GithubCredential;

/**
 * GitHub 자격 증명 저장 outbound port다.
 */
public interface SaveGithubCredentialPort {

    GithubCredential save(GithubCredential credential);
}
