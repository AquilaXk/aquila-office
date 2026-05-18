package com.aquilaoffice.adapter.out.persistence.github;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.domain.github.GithubCredential;
import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * GitHub 자격 증명을 github_credentials 테이블에 저장하는 JPA entity다.
 */
@Entity
@Table(name = "github_credentials")
public class GithubCredentialJpaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceJpaEntity workspace;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "token", nullable = false, columnDefinition = "TEXT")
    private String token;

    @Column(name = "created_by_google_sub", nullable = false, length = 120)
    private String createdByGoogleSub;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected GithubCredentialJpaEntity() {}

    private GithubCredentialJpaEntity(
            WorkspaceJpaEntity workspace,
            String displayName,
            String token,
            String createdByGoogleSub,
            LocalDateTime deletedAt) {
        this.workspace = requireWorkspaceEntity(workspace);
        this.displayName = displayName;
        this.token = token;
        this.createdByGoogleSub = createdByGoogleSub;
        this.deletedAt = deletedAt;
    }

    public static GithubCredentialJpaEntity from(
            GithubCredential credential,
            WorkspaceJpaEntity workspace,
            String encryptedToken) {
        // token column에는 암호화된 저장값만 남기고 원문은 persistence adapter에서 domain 복원 시에만 사용한다.
        return new GithubCredentialJpaEntity(
                workspace,
                credential.getDisplayName(),
                encryptedToken,
                credential.getCreatedByGoogleSub(),
                null);
    }

    public void update(String displayName, String encryptedToken) {
        this.displayName = displayName;
        this.token = encryptedToken;
    }

    public void softDelete(LocalDateTime deletedAt) {
        if (deletedAt == null) {
            throw new IllegalArgumentException("deletedAt must not be null");
        }
        if (this.deletedAt == null) {
            this.deletedAt = deletedAt;
        }
    }

    public WorkspaceJpaEntity getWorkspace() {
        return workspace;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getToken() {
        return token;
    }

    public String getCreatedByGoogleSub() {
        return createdByGoogleSub;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    private static WorkspaceJpaEntity requireWorkspaceEntity(WorkspaceJpaEntity workspace) {
        if (workspace == null) {
            throw new IllegalArgumentException("workspace entity must not be null");
        }
        return workspace;
    }
}
