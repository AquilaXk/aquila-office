package com.aquilaoffice.adapter.out.persistence.member;

import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.SaveGoogleLoginMemberPort;
import com.aquilaoffice.application.port.out.SavedGoogleLoginMember;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 조회 outbound port를 JPA repository로 구현하는 persistence adapter다.
 */
@Repository
public class MemberPersistenceAdapter implements LoadMemberPort, SaveGoogleLoginMemberPort {

    private final MemberJpaRepository memberJpaRepository;

    public MemberPersistenceAdapter(MemberJpaRepository memberJpaRepository) {
        this.memberJpaRepository = memberJpaRepository;
    }

    @Override
    public Optional<Member> findByGoogleSub(String googleSub) {
        if (googleSub == null || googleSub.isBlank()) {
            return Optional.empty();
        }
        // 외부 입력에서 넘어온 식별자는 저장소 조회 전에 공백을 제거해 같은 사용자를 안정적으로 찾는다.
        return memberJpaRepository.findByGoogleSub(googleSub.trim()).map(MemberJpaEntity::toDomain);
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

        MemberJpaEntity memberEntity = memberJpaRepository.findByGoogleSub(normalizedGoogleSub)
                .map(existing -> updateExisting(existing, normalizedEmail, normalizedName, role))
                .orElseGet(() -> createNew(normalizedGoogleSub, normalizedEmail, normalizedName, role));
        return toSavedMember(memberEntity);
    }

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
