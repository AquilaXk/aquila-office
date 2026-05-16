package com.aquilaoffice.adapter.out.persistence.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * refresh_tokens 테이블 접근을 담당하는 Spring Data repository다.
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {

    Optional<RefreshTokenJpaEntity> findByMemberId(Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshTokenJpaEntity rt
            set rt.token = :newToken
            where rt.memberId = :memberId
              and rt.token = :currentToken
            """)
    int rotateIfMatch(
            @Param("memberId") Long memberId,
            @Param("currentToken") String currentToken,
            @Param("newToken") String newToken);
}
