package com.aquilaoffice.adapter.out.persistence.auth;

import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * 회원별 refresh token digest를 로그인 세션 단위로 저장하는 JPA entity다.
 */
@Entity
@Table(
        name = "refresh_token_sessions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_refresh_token_sessions_session",
                columnNames = "session_id"))
public class RefreshTokenJpaEntity extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "token", nullable = false, length = 1000)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected RefreshTokenJpaEntity() {}

    private RefreshTokenJpaEntity(Long memberId, String sessionId, String token, Instant expiresAt) {
        this.memberId = requirePositive(memberId, "memberId");
        this.sessionId = requireNotBlank(sessionId, "sessionId");
        this.token = requireNotBlank(token, "token");
        this.expiresAt = requireNotNull(expiresAt, "expiresAt");
    }

    static RefreshTokenJpaEntity issue(Long memberId, String sessionId, String token, Instant expiresAt) {
        return new RefreshTokenJpaEntity(memberId, sessionId, token, expiresAt);
    }

    void rotate(String token, Instant expiresAt) {
        this.token = requireNotBlank(token, "token");
        this.expiresAt = requireNotNull(expiresAt, "expiresAt");
    }

    String getToken() {
        return token;
    }

    String getSessionId() {
        return sessionId;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static Instant requireNotNull(Instant value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }
}
