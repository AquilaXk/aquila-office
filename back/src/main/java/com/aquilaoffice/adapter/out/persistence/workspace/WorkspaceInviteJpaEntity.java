package com.aquilaoffice.adapter.out.persistence.workspace;

import com.aquilaoffice.domain.workspace.WorkspaceInvite;
import com.aquilaoffice.domain.workspace.WorkspaceInviteStatus;
import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * 워크스페이스 초대 토큰과 상태를 workspace_invites 테이블에 저장하는 JPA entity다.
 */
@Entity
@Table(name = "workspace_invites", uniqueConstraints = @UniqueConstraint(columnNames = "token"))
public class WorkspaceInviteJpaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceJpaEntity workspace;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "token", nullable = false, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkspaceInviteStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    protected WorkspaceInviteJpaEntity() {}

    private WorkspaceInviteJpaEntity(
            WorkspaceJpaEntity workspace,
            String email,
            String token,
            WorkspaceInviteStatus status,
            LocalDateTime createdAt,
            LocalDateTime expiresAt,
            LocalDateTime acceptedAt,
            LocalDateTime revokedAt) {
        this.workspace = requireWorkspaceEntity(workspace);
        this.email = email;
        this.token = token;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.acceptedAt = acceptedAt;
        this.revokedAt = revokedAt;
    }

    public static WorkspaceInviteJpaEntity from(WorkspaceInvite invite, WorkspaceJpaEntity workspace) {
        // 초대 도메인은 생성 시 검증되므로 persistence layer에서는 FK와 저장 컬럼만 조합한다.
        return new WorkspaceInviteJpaEntity(
                workspace,
                invite.getEmail(),
                invite.getToken(),
                invite.getStatus(),
                invite.getCreatedAt(),
                invite.getExpiresAt(),
                invite.getAcceptedAt(),
                invite.getRevokedAt());
    }

    public void accept(LocalDateTime acceptedAt) {
        if (acceptedAt == null) {
            throw new IllegalArgumentException("acceptedAt must not be null");
        }
        this.status = WorkspaceInviteStatus.ACCEPTED;
        this.acceptedAt = acceptedAt;
    }

    public void revoke(LocalDateTime revokedAt) {
        if (revokedAt == null) {
            throw new IllegalArgumentException("revokedAt must not be null");
        }
        if (status != WorkspaceInviteStatus.PENDING) {
            throw new IllegalStateException("invite is not pending");
        }
        this.status = WorkspaceInviteStatus.REVOKED;
        this.revokedAt = revokedAt;
    }

    public void extendExpiresAt(int additionalDays, LocalDateTime extendedAt) {
        if (additionalDays < 1 || additionalDays > 30) {
            throw new IllegalArgumentException("additionalDays must be between 1 and 30");
        }
        if (extendedAt == null) {
            throw new IllegalArgumentException("extendedAt must not be null");
        }
        if (status != WorkspaceInviteStatus.PENDING || !extendedAt.isBefore(expiresAt)) {
            throw new IllegalStateException("invite is not extendable");
        }
        this.expiresAt = expiresAt.plusDays(additionalDays);
    }

    public WorkspaceJpaEntity getWorkspace() {
        return workspace;
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    public WorkspaceInviteStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    private static WorkspaceJpaEntity requireWorkspaceEntity(WorkspaceJpaEntity workspace) {
        if (workspace == null) {
            throw new IllegalArgumentException("workspace entity must not be null");
        }
        return workspace;
    }
}
