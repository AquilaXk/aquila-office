package com.aquilaoffice.adapter.out.persistence.auth;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * refresh_tokens 테이블 접근을 담당하는 Spring Data repository다.
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {

    Optional<RefreshTokenJpaEntity> findByMemberIdAndSessionId(Long memberId, String sessionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshTokenJpaEntity rt
            set rt.token = :newToken
              , rt.expiresAt = :newExpiresAt
            where rt.memberId = :memberId
              and rt.sessionId = :sessionId
              and rt.token = :currentToken
              and rt.expiresAt > :now
            """)
    int rotateIfMatch(
            @Param("memberId") Long memberId,
            @Param("sessionId") String sessionId,
            @Param("currentToken") String currentToken,
            @Param("newToken") String newToken,
            @Param("newExpiresAt") Instant newExpiresAt,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from RefreshTokenJpaEntity rt
            where rt.memberId = :memberId
              and rt.sessionId = :sessionId
              and rt.token = :currentToken
            """)
    int deleteIfMatch(
            @Param("memberId") Long memberId,
            @Param("sessionId") String sessionId,
            @Param("currentToken") String currentToken);
}
