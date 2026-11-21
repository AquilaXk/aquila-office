package com.aquilaoffice.adapter.out.persistence.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * MemberPersistenceAdapter의 Bounded TTL 인메모리 캐시 hit, 만료, 상한 퇴거, 갱신 계약을 검증한다.
 */
class MemberPersistenceAdapterCacheTest {

    @Test
    void cachesRepeatedLookupsAcrossGoogleSubAndEmailUntilTtlExpires() {
        MemberJpaRepository repository = mock(MemberJpaRepository.class);
        MutableClock clock = new MutableClock(Instant.parse("2026-11-21T00:00:00Z"));
        MemberPersistenceAdapter adapter = new MemberPersistenceAdapter(repository, clock);

        MemberJpaEntity entity = MemberJpaEntity.create("sub-1", "one@example.com", "One", MemberRole.USER);
        ReflectionTestUtils.setField(entity, "id", 1L);
        given(repository.findByGoogleSub("sub-1")).willReturn(Optional.of(entity));

        assertThat(adapter.findByGoogleSub("sub-1")).isPresent();
        assertThat(adapter.findByGoogleSub("sub-1")).isPresent();
        assertThat(adapter.findByEmail("one@example.com")).isPresent();
        verify(repository, times(1)).findByGoogleSub("sub-1");
        verify(repository, times(0)).findByEmail(any());

        clock.advance(Duration.ofSeconds(30));
        assertThat(adapter.findByGoogleSub("sub-1")).isPresent();
        verify(repository, times(2)).findByGoogleSub("sub-1");
    }

    @Test
    void saveOrUpdateRefreshesCacheAndEvictsPreviousEmail() {
        MemberJpaRepository repository = mock(MemberJpaRepository.class);
        MutableClock clock = new MutableClock(Instant.parse("2026-11-21T00:00:00Z"));
        MemberPersistenceAdapter adapter = new MemberPersistenceAdapter(repository, clock);

        MemberJpaEntity existing = MemberJpaEntity.create("sub-2", "old@example.com", "Two", MemberRole.USER);
        ReflectionTestUtils.setField(existing, "id", 2L);
        given(repository.findByGoogleSub("sub-2")).willReturn(Optional.of(existing));
        given(repository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(repository.save(existing)).willReturn(existing);

        adapter.saveOrUpdate("sub-2", "new@example.com", "Two Admin", MemberRole.ADMIN);

        Optional<Member> bySub = adapter.findByGoogleSub("sub-2");
        assertThat(bySub).hasValueSatisfying(member -> {
            assertThat(member.getEmail()).isEqualTo("new@example.com");
            assertThat(member.getRole()).isEqualTo(MemberRole.ADMIN);
        });
        assertThat(adapter.findByEmail("new@example.com")).isPresent();
        // 저장 직후에는 DB 재조회 없이 캐시에서 즉시 반환되어야 한다.
        verify(repository, times(1)).findByGoogleSub("sub-2");

        given(repository.findByEmail("old@example.com")).willReturn(Optional.empty());
        assertThat(adapter.findByEmail("old@example.com")).isEmpty();
        verify(repository, times(1)).findByEmail("old@example.com");
    }

    @Test
    void cacheSizeRemainsBoundedAtMaxEntries() {
        MemberJpaRepository repository = mock(MemberJpaRepository.class);
        MutableClock clock = new MutableClock(Instant.parse("2026-11-21T00:00:00Z"));
        MemberPersistenceAdapter adapter = new MemberPersistenceAdapter(repository, clock);

        int totalToInsert = MemberPersistenceAdapter.MAX_CACHE_ENTRIES + 50;
        for (int index = 0; index < totalToInsert; index++) {
            String googleSub = "sub-" + index;
            String email = "user" + index + "@example.com";
            MemberJpaEntity entity = MemberJpaEntity.create(googleSub, email, "User " + index, MemberRole.USER);
            ReflectionTestUtils.setField(entity, "id", (long) index + 1L);
            given(repository.findByGoogleSub(googleSub)).willReturn(Optional.of(entity));
            assertThat(adapter.findByGoogleSub(googleSub)).isPresent();
        }

        assertThat(adapter.googleSubCacheSize()).isLessThanOrEqualTo(MemberPersistenceAdapter.MAX_CACHE_ENTRIES);
        assertThat(adapter.emailCacheSize()).isLessThanOrEqualTo(MemberPersistenceAdapter.MAX_CACHE_ENTRIES);
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;

        private MutableClock(Instant initial) {
            this.instant = new AtomicReference<>(initial);
        }

        private void advance(Duration duration) {
            instant.updateAndGet(current -> current.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
