package com.aquilaoffice.adapter.out.persistence.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.out.DeleteRefreshTokenPort;
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
    private DeleteRefreshTokenPort deleteRefreshTokenPort;

    @Autowired
    private RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Test
    void saveOrRotateKeepsIndependentSessionsForSameMember() {
        saveRefreshTokenPort.saveOrRotate(1L, "session-a", " old-refresh-a ", OLD_EXPIRES_AT);
        saveRefreshTokenPort.saveOrRotate(1L, "session-b", " old-refresh-b ", OLD_EXPIRES_AT);

        saveRefreshTokenPort.saveOrRotate(1L, "session-a", " new-refresh-a ", NEW_EXPIRES_AT);

        RefreshTokenJpaEntity sessionA = refreshTokenJpaRepository.findByMemberIdAndSessionId(1L, "session-a")
                .orElseThrow();
        RefreshTokenJpaEntity sessionB = refreshTokenJpaRepository.findByMemberIdAndSessionId(1L, "session-b")
                .orElseThrow();
        assertThat(sessionA.getToken())
                .startsWith("sha256:")
                .isNotEqualTo("new-refresh-a");
        assertThat(sessionA.getExpiresAt()).isEqualTo(NEW_EXPIRES_AT);
        assertThat(sessionB.getToken())
                .startsWith("sha256:")
                .isNotEqualTo("old-refresh-b");
        assertThat(sessionB.getExpiresAt()).isEqualTo(OLD_EXPIRES_AT);
        assertThat(refreshTokenJpaRepository.findAll()).hasSize(2);
    }

    @Test
    void saveOrRotateDoesNotStoreRawRefreshToken() {
        saveRefreshTokenPort.saveOrRotate(9L, "session-a", " raw-refresh-token ", OLD_EXPIRES_AT);

        RefreshTokenJpaEntity saved = refreshTokenJpaRepository.findByMemberIdAndSessionId(9L, "session-a")
                .orElseThrow();

        assertThat(saved.getToken())
                .startsWith("sha256:")
                .isNotEqualTo("raw-refresh-token");
    }

    @Test
    void rotateIfMatchUpdatesOnlyCurrentSessionRefreshToken() {
        saveRefreshTokenPort.saveOrRotate(2L, "session-a", "old-refresh", OLD_EXPIRES_AT);
        saveRefreshTokenPort.saveOrRotate(2L, "session-b", "other-refresh", OLD_EXPIRES_AT);

        boolean rotated = rotateRefreshTokenPort.rotateIfMatch(
                2L,
                "session-a",
                "old-refresh",
                "new-refresh",
                NEW_EXPIRES_AT,
                NOW);

        assertThat(rotated).isTrue();
        RefreshTokenJpaEntity sessionA = refreshTokenJpaRepository.findByMemberIdAndSessionId(2L, "session-a")
                .orElseThrow();
        RefreshTokenJpaEntity sessionB = refreshTokenJpaRepository.findByMemberIdAndSessionId(2L, "session-b")
                .orElseThrow();
        assertThat(sessionA.getToken())
                .startsWith("sha256:")
                .isNotEqualTo("new-refresh");
        assertThat(sessionA.getExpiresAt()).isEqualTo(NEW_EXPIRES_AT);
        assertThat(sessionB.getToken())
                .startsWith("sha256:")
                .isNotEqualTo("other-refresh");
        assertThat(sessionB.getExpiresAt()).isEqualTo(OLD_EXPIRES_AT);
    }

    @Test
    void rotateIfMatchRejectsExpiredStoredRefreshToken() {
        saveRefreshTokenPort.saveOrRotate(3L, "session-a", "expired-refresh", NOW.minusSeconds(1));

        boolean rotated = rotateRefreshTokenPort.rotateIfMatch(
                3L,
                "session-a",
                "expired-refresh",
                "new-refresh",
                NEW_EXPIRES_AT,
                NOW);

        assertThat(rotated).isFalse();
        RefreshTokenJpaEntity saved = refreshTokenJpaRepository.findByMemberIdAndSessionId(3L, "session-a")
                .orElseThrow();
        assertThat(saved.getToken())
                .startsWith("sha256:")
                .isNotEqualTo("expired-refresh");
        assertThat(saved.getExpiresAt()).isEqualTo(NOW.minusSeconds(1));
    }

    @Test
    void deleteIfMatchDeletesOnlyCurrentSessionRefreshToken() {
        saveRefreshTokenPort.saveOrRotate(4L, "session-a", "old-refresh", OLD_EXPIRES_AT);
        saveRefreshTokenPort.saveOrRotate(4L, "session-b", "other-refresh", OLD_EXPIRES_AT);

        boolean deleted = deleteRefreshTokenPort.deleteIfMatch(4L, "session-a", "old-refresh");

        assertThat(deleted).isTrue();
        assertThat(refreshTokenJpaRepository.findByMemberIdAndSessionId(4L, "session-a")).isEmpty();
        assertThat(refreshTokenJpaRepository.findByMemberIdAndSessionId(4L, "session-b")).isPresent();
    }

    @Test
    void rotateIfMatchMigratesLegacyPlaintextRefreshToken() {
        refreshTokenJpaRepository.save(RefreshTokenJpaEntity.issue(
                5L,
                "session-a",
                "legacy-refresh",
                OLD_EXPIRES_AT));

        boolean rotated = rotateRefreshTokenPort.rotateIfMatch(
                5L,
                "session-a",
                "legacy-refresh",
                "new-refresh",
                NEW_EXPIRES_AT,
                NOW);

        assertThat(rotated).isTrue();
        RefreshTokenJpaEntity saved = refreshTokenJpaRepository.findByMemberIdAndSessionId(5L, "session-a")
                .orElseThrow();
        assertThat(saved.getToken())
                .startsWith("sha256:")
                .isNotEqualTo("new-refresh");
        assertThat(saved.getExpiresAt()).isEqualTo(NEW_EXPIRES_AT);
    }

    @Test
    void deleteIfMatchSupportsLegacyPlaintextRefreshToken() {
        refreshTokenJpaRepository.save(RefreshTokenJpaEntity.issue(
                6L,
                "session-a",
                "legacy-refresh",
                OLD_EXPIRES_AT));

        boolean deleted = deleteRefreshTokenPort.deleteIfMatch(6L, "session-a", "legacy-refresh");

        assertThat(deleted).isTrue();
        assertThat(refreshTokenJpaRepository.findByMemberIdAndSessionId(6L, "session-a")).isEmpty();
    }
}
