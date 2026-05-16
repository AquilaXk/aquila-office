package com.aquilaoffice.adapter.out.persistence.auth;

import com.aquilaoffice.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 회원별 현재 refresh token 값을 저장하는 JPA entity다.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity extends BaseEntity {

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(name = "token", nullable = false, length = 1000)
    private String token;

    protected RefreshTokenJpaEntity() {}

    private RefreshTokenJpaEntity(Long memberId, String token) {
        this.memberId = requirePositive(memberId, "memberId");
        this.token = requireNotBlank(token, "token");
    }

    static RefreshTokenJpaEntity issue(Long memberId, String token) {
        return new RefreshTokenJpaEntity(memberId, token);
    }

    void rotate(String token) {
        this.token = requireNotBlank(token, "token");
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
}
