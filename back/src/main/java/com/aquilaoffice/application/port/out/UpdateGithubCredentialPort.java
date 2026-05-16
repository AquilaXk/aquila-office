package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.github.GithubCredential;

/**
 * GitHub 자격 증명 수정 outbound port다.
 */
public interface UpdateGithubCredentialPort {

    GithubCredential update(GithubCredential credential);
}
