package com.aquilaoffice.domain.workspace;

import java.time.LocalDateTime;

/**
 * 워크스페이스 멤버 초대 토큰과 상태 전이를 관리하는 순수 도메인 모델이다.
 */
public class WorkspaceInvite {

    private final Workspace workspace;
    private final String email;
    private final String token;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;
    private WorkspaceInviteStatus status;
    private LocalDateTime acceptedAt;

    private WorkspaceInvite(
            Workspace workspace,
            String email,
            String token,
            LocalDateTime createdAt,
            LocalDateTime expiresAt) {
        this.workspace = requireWorkspace(workspace);
        this.email = requireNotBlank(email, "email");
        this.token = requireNotBlank(token, "token");
        this.createdAt = requireCreatedAt(createdAt);
        this.expiresAt = requireExpiresAt(expiresAt, createdAt);
        this.status = WorkspaceInviteStatus.PENDING;
    }

    public static WorkspaceInvite create(
            Workspace workspace,
            String email,
            String token,
            LocalDateTime createdAt,
            LocalDateTime expiresAt) {
        return new WorkspaceInvite(workspace, email, token, createdAt, expiresAt);
    }

    public void accept(LocalDateTime acceptedAt) {
        LocalDateTime requestedAcceptedAt = requireAcceptedAt(acceptedAt);
        if (isExpiredAt(requestedAcceptedAt)) {
            throw new IllegalStateException("invite is expired");
        }
        if (status != WorkspaceInviteStatus.PENDING) {
            throw new IllegalStateException("invite is not pending");
        }
        this.status = WorkspaceInviteStatus.ACCEPTED;
        this.acceptedAt = requestedAcceptedAt;
    }

    public void expire(LocalDateTime now) {
        LocalDateTime requestedNow = requireNow(now);
        // 이미 처리된 초대는 이후 만료 배치가 실행되어도 최종 상태를 보존한다.
        if (status == WorkspaceInviteStatus.PENDING && !requestedNow.isBefore(expiresAt)) {
            this.status = WorkspaceInviteStatus.EXPIRED;
        }
    }

    public boolean isExpiredAt(LocalDateTime now) {
        LocalDateTime requestedNow = requireNow(now);
        if (status == WorkspaceInviteStatus.ACCEPTED) {
            return false;
        }
        return status == WorkspaceInviteStatus.EXPIRED || !requestedNow.isBefore(expiresAt);
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public WorkspaceInviteStatus getStatus() {
        return status;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    private static Workspace requireWorkspace(Workspace workspace) {
        if (workspace == null) {
            throw new IllegalArgumentException("workspace must not be null");
        }
        return workspace;
    }

    private static LocalDateTime requireCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        return createdAt;
    }

    private static LocalDateTime requireExpiresAt(LocalDateTime expiresAt, LocalDateTime createdAt) {
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt must not be null");
        }
        if (createdAt != null && !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt must be after createdAt");
        }
        return expiresAt;
    }

    private static LocalDateTime requireAcceptedAt(LocalDateTime acceptedAt) {
        if (acceptedAt == null) {
            throw new IllegalArgumentException("acceptedAt must not be null");
        }
        return acceptedAt;
    }

    private static LocalDateTime requireNow(LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        return now;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
