package com.aquilaoffice.adapter.out.persistence.member;

import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.SaveGoogleLoginMemberPort;
import com.aquilaoffice.application.port.out.SavedGoogleLoginMember;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import java.time.Clock;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 조회 outbound port를 JPA repository와 Bounded TTL 캐시로 구현하는 persistence adapter다.
 */
@Repository
public class MemberPersistenceAdapter implements LoadMemberPort, SaveGoogleLoginMemberPort {

    static final int MAX_CACHE_ENTRIES = 4_096;
    static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private final MemberJpaRepository memberJpaRepository;
    private final Clock clock;
    private final ConcurrentHashMap<String, CacheEntry> byGoogleSubCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheEntry> byEmailCache = new ConcurrentHashMap<>();

    @Autowired
    public MemberPersistenceAdapter(MemberJpaRepository memberJpaRepository) {
        this(memberJpaRepository, Clock.systemUTC());
    }

    MemberPersistenceAdapter(MemberJpaRepository memberJpaRepository, Clock clock) {
        this.memberJpaRepository = Objects.requireNonNull(memberJpaRepository, "memberJpaRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Optional<Member> findByGoogleSub(String googleSub) {
        if (googleSub == null || googleSub.isBlank()) {
            return Optional.empty();
        }
        String normalizedGoogleSub = googleSub.trim();
        Member cached = getCached(byGoogleSubCache, normalizedGoogleSub);
        if (cached != null) {
            return Optional.of(cached);
        }
        // 캐시 미스 시에만 저장소를 조회하고 google_sub/email 양쪽 색인을 함께 갱신한다.
        Optional<Member> loaded = memberJpaRepository.findByGoogleSub(normalizedGoogleSub)
                .map(MemberJpaEntity::toDomain);
        loaded.ifPresent(this::cacheMember);
        return loaded;
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String normalizedEmail = email.trim();
        Member cached = getCached(byEmailCache, normalizedEmail);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<Member> loaded = memberJpaRepository.findByEmail(normalizedEmail)
                .map(MemberJpaEntity::toDomain);
        loaded.ifPresent(this::cacheMember);
        return loaded;
    }

    @Override
    @Transactional
    public SavedGoogleLoginMember saveOrUpdate(String googleSub, String email, String name, MemberRole role) {
        String normalizedGoogleSub = requireNotBlank(googleSub, "googleSub");
        String normalizedEmail = requireNotBlank(email, "email");
        String normalizedName = requireNotBlank(name, "name");
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }

        Optional<MemberJpaEntity> existingOptional = memberJpaRepository.findByGoogleSub(normalizedGoogleSub);
        String previousEmail = existingOptional.map(MemberJpaEntity::getEmail).orElse(null);
        MemberJpaEntity memberEntity = existingOptional
                .map(existing -> updateExisting(existing, normalizedEmail, normalizedName, role))
                .orElseGet(() -> createNew(normalizedGoogleSub, normalizedEmail, normalizedName, role));
        if (previousEmail != null && !previousEmail.equals(memberEntity.getEmail())) {
            byEmailCache.remove(previousEmail);
        }
        cacheMember(memberEntity.toDomain());
        return toSavedMember(memberEntity);
    }

    int googleSubCacheSize() {
        return byGoogleSubCache.size();
    }

    int emailCacheSize() {
        return byEmailCache.size();
    }

    private Member getCached(ConcurrentHashMap<String, CacheEntry> cache, String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAtMillis() <= clock.millis()) {
            cache.remove(key, entry);
            return null;
        }
        return entry.member();
    }

    private void cacheMember(Member member) {
        long nowMillis = clock.millis();
        CacheEntry entry = new CacheEntry(member, nowMillis + CACHE_TTL.toMillis());
        putBounded(byGoogleSubCache, member.getGoogleSub(), entry, nowMillis);
        putBounded(byEmailCache, member.getEmail(), entry, nowMillis);
    }

    private void putBounded(
            ConcurrentHashMap<String, CacheEntry> cache,
            String key,
            CacheEntry entry,
            long nowMillis) {
        if (cache.size() >= MAX_CACHE_ENTRIES && !cache.containsKey(key)) {
            evictExpiredAndOverflow(cache, nowMillis);
        }
        cache.put(key, entry);
        if (cache.size() > MAX_CACHE_ENTRIES) {
            trimOverflow(cache, key);
        }
    }

    private void evictExpiredAndOverflow(ConcurrentHashMap<String, CacheEntry> cache, long nowMillis) {
        cache.entrySet().removeIf(candidate -> candidate.getValue().expiresAtMillis() <= nowMillis);
        trimOverflow(cache, null);
    }

    private void trimOverflow(ConcurrentHashMap<String, CacheEntry> cache, String protectedKey) {
        if (cache.size() < MAX_CACHE_ENTRIES) {
            return;
        }
        Iterator<Map.Entry<String, CacheEntry>> iterator = cache.entrySet().iterator();
        while (cache.size() >= MAX_CACHE_ENTRIES && iterator.hasNext()) {
            String candidateKey = iterator.next().getKey();
            if (protectedKey == null || !protectedKey.equals(candidateKey)) {
                iterator.remove();
            }
        }
    }

    private record CacheEntry(Member member, long expiresAtMillis) {}

    private MemberJpaEntity updateExisting(
            MemberJpaEntity existing,
            String email,
            String name,
            MemberRole role) {
        validateEmailConflict(email, existing.getId());
        existing.updateGoogleLoginProfile(email, name, role);
        return memberJpaRepository.save(existing);
    }

    private MemberJpaEntity createNew(String googleSub, String email, String name, MemberRole role) {
        validateEmailConflict(email, null);
        return memberJpaRepository.save(MemberJpaEntity.create(googleSub, email, name, role));
    }

    private void validateEmailConflict(String email, Long currentMemberId) {
        memberJpaRepository.findByEmail(email).ifPresent(existing -> {
            if (currentMemberId == null || !currentMemberId.equals(existing.getId())) {
                throw new IllegalArgumentException("email already exists");
            }
        });
    }

    private SavedGoogleLoginMember toSavedMember(MemberJpaEntity memberEntity) {
        return new SavedGoogleLoginMember(
                memberEntity.getId(),
                memberEntity.getGoogleSub(),
                memberEntity.getEmail(),
                memberEntity.getName(),
                memberEntity.getRole());
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
