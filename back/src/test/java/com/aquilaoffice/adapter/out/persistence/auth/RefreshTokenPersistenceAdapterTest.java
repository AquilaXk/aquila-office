package com.aquilaoffice.adapter.out.persistence.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.out.RotateRefreshTokenPort;
import com.aquilaoffice.application.port.out.SaveRefreshTokenPort;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Refresh token 저장소가 token 값과 만료 시각을 함께 회전하는지 검증한다.
 */
@DataJpaTest
@Import(RefreshTokenPersistenceAdapter.class)
class RefreshTokenPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-05-18T00:00:00Z");
    private static final Instant OLD_EXPIRES_AT = Instant.parse("2026-06-01T00:00:00Z");
    private static final Instant NEW_EXPIRES_AT = Instant.parse("2026-06-02T00:00:00Z");

    @Autowired
    private SaveRefreshTokenPort saveRefreshTokenPort;

    @Autowired
    private RotateRefreshTokenPort rotateRefreshTokenPort;

    @Autowired
    private RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Test
    void saveOrRotatePersistsRefreshTokenExpiry() {
        saveRefreshTokenPort.saveOrRotate(1L, " old-refresh ", OLD_EXPIRES_AT);

        RefreshTokenJpaEntity saved = refreshTokenJpaRepository.findByMemberId(1L).orElseThrow();
        assertThat(saved.getToken()).isEqualTo("old-refresh");
        assertThat(saved.getExpiresAt()).isEqualTo(OLD_EXPIRES_AT);

        saveRefreshTokenPort.saveOrRotate(1L, " new-refresh ", NEW_EXPIRES_AT);

        RefreshTokenJpaEntity rotated = refreshTokenJpaRepository.findByMemberId(1L).orElseThrow();
        assertThat(rotated.getToken()).isEqualTo("new-refresh");
        assertThat(rotated.getExpiresAt()).isEqualTo(NEW_EXPIRES_AT);
    }

    @Test
    void rotateIfMatchUpdatesOnlyCurrentAndNotExpiredRefreshToken() {
        saveRefreshTokenPort.saveOrRotate(2L, "old-refresh", OLD_EXPIRES_AT);

        boolean rotated = rotateRefreshTokenPort.rotateIfMatch(
                2L,
                "old-refresh",
                "new-refresh",
                NEW_EXPIRES_AT,
                NOW);

        assertThat(rotated).isTrue();
        RefreshTokenJpaEntity saved = refreshTokenJpaRepository.findByMemberId(2L).orElseThrow();
        assertThat(saved.getToken()).isEqualTo("new-refresh");
        assertThat(saved.getExpiresAt()).isEqualTo(NEW_EXPIRES_AT);
    }

    @Test
    void rotateIfMatchRejectsExpiredStoredRefreshToken() {
        saveRefreshTokenPort.saveOrRotate(3L, "expired-refresh", NOW.minusSeconds(1));

        boolean rotated = rotateRefreshTokenPort.rotateIfMatch(
                3L,
                "expired-refresh",
                "new-refresh",
                NEW_EXPIRES_AT,
                NOW);

        assertThat(rotated).isFalse();
        RefreshTokenJpaEntity saved = refreshTokenJpaRepository.findByMemberId(3L).orElseThrow();
        assertThat(saved.getToken()).isEqualTo("expired-refresh");
        assertThat(saved.getExpiresAt()).isEqualTo(NOW.minusSeconds(1));
    }
}
