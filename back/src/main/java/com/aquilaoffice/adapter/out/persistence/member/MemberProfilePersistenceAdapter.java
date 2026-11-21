package com.aquilaoffice.adapter.out.persistence.member;

import com.aquilaoffice.application.port.in.MemberAvatarColorsResult;
import com.aquilaoffice.application.port.in.MemberProfileResult;
import com.aquilaoffice.application.port.out.InitializeMemberProfilePort;
import com.aquilaoffice.application.port.out.LoadMemberProfilePort;
import com.aquilaoffice.application.port.out.MemberProfileUpdate;
import com.aquilaoffice.application.port.out.UpdateMemberProfilePort;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 멤버 프로필 outbound port를 JPA repository 조합으로 구현한다.
 */
@Repository
public class MemberProfilePersistenceAdapter
        implements InitializeMemberProfilePort, LoadMemberProfilePort, UpdateMemberProfilePort {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberProfileJpaRepository memberProfileJpaRepository;

    public MemberProfilePersistenceAdapter(
            MemberJpaRepository memberJpaRepository,
            MemberProfileJpaRepository memberProfileJpaRepository) {
        this.memberJpaRepository = memberJpaRepository;
        this.memberProfileJpaRepository = memberProfileJpaRepository;
    }

    @Override
    @Transactional
    public void initializeDefaultProfile(String googleSub) {
        String normalizedGoogleSub = normalizeGoogleSub(googleSub);
        memberJpaRepository.findByGoogleSub(normalizedGoogleSub).ifPresent(this::findOrCreateProfile);
    }

    @Override
    @Transactional
    public Optional<MemberProfileResult> findOrCreateDefaultByGoogleSub(String googleSub) {
        String normalizedGoogleSub = normalizeGoogleSub(googleSub);
        return memberJpaRepository
                .findByGoogleSub(normalizedGoogleSub)
                .map(this::findOrCreateProfile)
                .map(this::toResult);
    }

    @Override
    @Transactional
    public Optional<MemberProfileResult> update(MemberProfileUpdate update) {
        Objects.requireNonNull(update, "update must not be null");
        String normalizedGoogleSub = normalizeGoogleSub(update.googleSub());

        return memberJpaRepository.findByGoogleSub(normalizedGoogleSub).map(member -> {
            MemberProfileJpaEntity profile = findOrCreateProfile(member);
            profile.update(
                    update.displayName(),
                    update.avatarKind(),
                    update.skinColor(),
                    update.hairColor(),
                    update.shirtColor());
            return toResult(profile);
        });
    }

    private MemberProfileJpaEntity findOrCreateProfile(MemberJpaEntity member) {
        return memberProfileJpaRepository
                .findByMember_GoogleSub(member.getGoogleSub())
                .orElseGet(() -> memberProfileJpaRepository.save(MemberProfileJpaEntity.createDefault(member)));
    }

    private MemberProfileResult toResult(MemberProfileJpaEntity profile) {
        return new MemberProfileResult(
                profile.getMember().getId(),
                profile.getDisplayName(),
                profile.getAvatarKind(),
                new MemberAvatarColorsResult(
                        profile.getAvatarSkinColor(),
                        profile.getAvatarHairColor(),
                        profile.getAvatarShirtColor()));
    }

    private String normalizeGoogleSub(String googleSub) {
        if (googleSub == null || googleSub.isBlank()) {
            throw new IllegalArgumentException("googleSub must not be blank");
        }
        return googleSub.trim();
    }
}
