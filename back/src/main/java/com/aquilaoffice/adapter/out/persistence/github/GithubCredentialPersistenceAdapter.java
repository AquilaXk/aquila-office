package com.aquilaoffice.adapter.out.persistence.github;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.DeleteGithubCredentialPort;
import com.aquilaoffice.application.port.out.ExistsGithubCredentialPort;
import com.aquilaoffice.application.port.out.LoadGithubCredentialPort;
import com.aquilaoffice.application.port.out.LoadGithubCredentialsPort;
import com.aquilaoffice.application.port.out.SaveGithubCredentialPort;
import com.aquilaoffice.application.port.out.UpdateGithubCredentialPort;
import com.aquilaoffice.domain.github.GithubCredential;
import com.aquilaoffice.global.security.ExternalTokenCipher;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * GitHub 자격 증명 outbound port를 JPA repository 조합으로 구현하는 persistence adapter다.
 */
@Repository
public class GithubCredentialPersistenceAdapter
        implements SaveGithubCredentialPort,
                LoadGithubCredentialsPort,
                LoadGithubCredentialPort,
                ExistsGithubCredentialPort,
                UpdateGithubCredentialPort,
                DeleteGithubCredentialPort {

    private final WorkspaceJpaRepository workspaceJpaRepository;
    private final GithubCredentialJpaRepository githubCredentialJpaRepository;
    private final ExternalTokenCipher externalTokenCipher;

    public GithubCredentialPersistenceAdapter(
            WorkspaceJpaRepository workspaceJpaRepository,
            GithubCredentialJpaRepository githubCredentialJpaRepository,
            ExternalTokenCipher externalTokenCipher) {
        this.workspaceJpaRepository = workspaceJpaRepository;
        this.githubCredentialJpaRepository = githubCredentialJpaRepository;
        this.externalTokenCipher = externalTokenCipher;
    }

    @Override
    @Transactional
    public GithubCredential save(GithubCredential credential) {
        Objects.requireNonNull(credential, "credential must not be null");
        WorkspaceJpaEntity workspaceEntity = workspaceJpaRepository
                .findActiveWorkspaceById(credential.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("workspace entity not found"));

        GithubCredentialJpaEntity savedEntity =
                githubCredentialJpaRepository.save(GithubCredentialJpaEntity.from(
                        credential,
                        workspaceEntity,
                        externalTokenCipher.encrypt(credential.getToken())));
        return toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GithubCredential> findActiveByWorkspaceId(Long workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");

        return githubCredentialJpaRepository.findActiveByWorkspaceIdOrderByIdDesc(workspaceId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GithubCredential> findActiveByIdAndWorkspaceId(Long credentialId, Long workspaceId) {
        Objects.requireNonNull(credentialId, "credentialId must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");

        return githubCredentialJpaRepository.findActiveByIdAndWorkspaceId(credentialId, workspaceId)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveDisplayName(Long workspaceId, String displayName) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");

        return githubCredentialJpaRepository.existsActiveDisplayName(workspaceId, displayName);
    }

    @Override
    @Transactional
    public GithubCredential update(GithubCredential credential) {
        Objects.requireNonNull(credential, "credential must not be null");
        GithubCredentialJpaEntity credentialEntity = githubCredentialJpaRepository
                .findActiveByIdAndWorkspaceId(credential.getId(), credential.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("github credential entity not found"));

        credentialEntity.update(
                credential.getDisplayName(),
                externalTokenCipher.encrypt(credential.getToken()));
        return toDomain(credentialEntity);
    }

    @Override
    @Transactional
    public void delete(Long credentialId, Long workspaceId) {
        Objects.requireNonNull(credentialId, "credentialId must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        GithubCredentialJpaEntity credentialEntity = githubCredentialJpaRepository
                .findActiveByIdAndWorkspaceId(credentialId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("github credential entity not found"));

        credentialEntity.softDelete(LocalDateTime.now());
    }

    private GithubCredential toDomain(GithubCredentialJpaEntity entity) {
        return GithubCredential.restore(
                entity.getId(),
                entity.getWorkspace().getId(),
                entity.getDisplayName(),
                externalTokenCipher.decrypt(entity.getToken()),
                entity.getCreatedByGoogleSub());
    }
}
