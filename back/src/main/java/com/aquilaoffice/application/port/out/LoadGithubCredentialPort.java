package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.github.GithubCredential;
import java.util.Optional;

/**
 * GitHub 자격 증명 단건 조회 outbound port다.
 */
public interface LoadGithubCredentialPort {

    Optional<GithubCredential> findActiveByIdAndWorkspaceId(Long credentialId, Long workspaceId);
}
