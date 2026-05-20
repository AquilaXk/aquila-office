package com.aquilaoffice.adapter.out.persistence.auth;

import com.aquilaoffice.application.port.out.DeleteRefreshTokenPort;
import com.aquilaoffice.application.port.out.RotateRefreshTokenPort;
import com.aquilaoffice.application.port.out.SaveRefreshTokenPort;
import java.time.Instant;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * refresh token 저장/회전 outbound port를 JPA로 구현하는 persistence adapter다.
 */
@Repository
public class RefreshTokenPersistenceAdapter implements SaveRefreshTokenPort, RotateRefreshTokenPort, DeleteRefreshTokenPort {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    public RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository refreshTokenJpaRepository) {
        this.refreshTokenJpaRepository = refreshTokenJpaRepository;
    }

    @Override
    @Transactional
    public void saveOrRotate(
            Long memberId,
            String refreshSessionId,
            String refreshToken,
            Instant refreshTokenExpiresAt) {
        String refreshTokenHash = RefreshTokenHash.hash(refreshToken);
        RefreshTokenJpaEntity entity = refreshTokenJpaRepository.findByMemberIdAndSessionId(memberId, refreshSessionId)
                .map(existing -> {
                    existing.rotate(refreshTokenHash, refreshTokenExpiresAt);
                    return existing;
                })
                .orElseGet(() -> RefreshTokenJpaEntity.issue(
                        memberId,
                        refreshSessionId,
                        refreshTokenHash,
                        refreshTokenExpiresAt));
        refreshTokenJpaRepository.save(entity);
    }

    @Override
    @Transactional
    public boolean rotateIfMatch(
            Long memberId,
            String refreshSessionId,
            String currentRefreshToken,
            String newRefreshToken,
            Instant newRefreshTokenExpiresAt,
            Instant now) {
        String currentRefreshTokenRaw = RefreshTokenHash.normalizeRaw(currentRefreshToken);
        return refreshTokenJpaRepository.rotateIfMatch(
                memberId,
                refreshSessionId,
                RefreshTokenHash.hash(currentRefreshTokenRaw),
                currentRefreshTokenRaw,
                RefreshTokenHash.hash(newRefreshToken),
                newRefreshTokenExpiresAt,
                now) == 1;
    }

    @Override
    @Transactional
    public boolean deleteIfMatch(Long memberId, String refreshSessionId, String refreshToken) {
        String refreshTokenRaw = RefreshTokenHash.normalizeRaw(refreshToken);
        return refreshTokenJpaRepository.deleteIfMatch(
                memberId,
                refreshSessionId,
                RefreshTokenHash.hash(refreshTokenRaw),
                refreshTokenRaw) == 1;
    }
}
