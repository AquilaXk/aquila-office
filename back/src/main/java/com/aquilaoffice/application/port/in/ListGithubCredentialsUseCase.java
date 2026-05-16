package com.aquilaoffice.application.port.in;

import java.util.List;

/**
 * GitHub 자격 증명 목록 조회 inbound port다.
 */
public interface ListGithubCredentialsUseCase {

    List<ListGithubCredentialsResult> listGithubCredentials(ListGithubCredentialsQuery query);
}
