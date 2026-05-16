package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.github.GithubCredential;
import java.util.List;

/**
 * GitHub 자격 증명 목록 조회 outbound port다.
 */
public interface LoadGithubCredentialsPort {

    List<GithubCredential> findActiveByWorkspaceId(Long workspaceId);
}
