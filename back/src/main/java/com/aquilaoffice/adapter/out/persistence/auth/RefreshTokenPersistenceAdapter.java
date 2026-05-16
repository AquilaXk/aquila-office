package com.aquilaoffice.adapter.out.persistence.auth;

import com.aquilaoffice.application.port.out.RotateRefreshTokenPort;
import com.aquilaoffice.application.port.out.SaveRefreshTokenPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * refresh token 저장/회전 outbound port를 JPA로 구현하는 persistence adapter다.
 */
@Repository
public class RefreshTokenPersistenceAdapter implements SaveRefreshTokenPort, RotateRefreshTokenPort {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    public RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository refreshTokenJpaRepository) {
        this.refreshTokenJpaRepository = refreshTokenJpaRepository;
    }

    @Override
    @Transactional
    public void saveOrRotate(Long memberId, String refreshToken) {
        RefreshTokenJpaEntity entity = refreshTokenJpaRepository.findByMemberId(memberId)
                .map(existing -> {
                    existing.rotate(refreshToken);
                    return existing;
                })
                .orElseGet(() -> RefreshTokenJpaEntity.issue(memberId, refreshToken));
        refreshTokenJpaRepository.save(entity);
    }

    @Override
    @Transactional
    public boolean rotateIfMatch(Long memberId, String currentRefreshToken, String newRefreshToken) {
        return refreshTokenJpaRepository.rotateIfMatch(memberId, currentRefreshToken, newRefreshToken) == 1;
    }
}
