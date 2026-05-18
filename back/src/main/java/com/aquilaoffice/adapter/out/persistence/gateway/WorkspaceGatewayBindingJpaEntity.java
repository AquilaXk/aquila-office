package com.aquilaoffice.adapter.out.persistence.gateway;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;
import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * workspace gateway binding을 workspace_gateway_bindings 테이블에 저장하는 JPA entity다.
 */
@Entity
@Table(name = "workspace_gateway_bindings")
public class WorkspaceGatewayBindingJpaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceJpaEntity workspace;

    @Column(name = "gateway_url", nullable = false, length = 300)
    private String gatewayUrl;

    @Column(name = "token", nullable = false, columnDefinition = "TEXT")
    private String token;

    @Column(name = "created_by_google_sub", nullable = false, length = 120)
    private String createdByGoogleSub;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected WorkspaceGatewayBindingJpaEntity() {}

    private WorkspaceGatewayBindingJpaEntity(
            WorkspaceJpaEntity workspace,
            String gatewayUrl,
            String token,
            String createdByGoogleSub,
            LocalDateTime deletedAt) {
        this.workspace = requireWorkspaceEntity(workspace);
        this.gatewayUrl = gatewayUrl;
        this.token = token;
        this.createdByGoogleSub = createdByGoogleSub;
        this.deletedAt = deletedAt;
    }

    public static WorkspaceGatewayBindingJpaEntity from(
            WorkspaceGatewayBinding binding,
            WorkspaceJpaEntity workspace,
            String encryptedToken) {
        // token column에는 암호화된 저장값만 남기고 web/API 응답은 기존처럼 maskedToken으로 제한한다.
        return new WorkspaceGatewayBindingJpaEntity(
                workspace,
                binding.getGatewayUrl(),
                encryptedToken,
                binding.getCreatedByGoogleSub(),
                null);
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

    public String getGatewayUrl() {
        return gatewayUrl;
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
