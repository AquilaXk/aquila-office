package com.aquilaoffice.adapter.out.persistence.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.out.InitializeMemberProfilePort;
import com.aquilaoffice.domain.member.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 멤버 프로필 persistence adapter가 로그인 직후 기본 프로필을 준비하는지 검증한다.
 */
@DataJpaTest
@Import(MemberProfilePersistenceAdapter.class)
class MemberProfilePersistenceAdapterTest {

    @Autowired
    private InitializeMemberProfilePort initializeMemberProfilePort;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private MemberProfileJpaRepository memberProfileJpaRepository;

    @Test
    void initializeDefaultProfileCreatesProfileForExistingMember() {
        memberJpaRepository.save(MemberJpaEntity.create(
                "google-1",
                "user@example.com",
                "User One",
                MemberRole.USER));

        initializeMemberProfilePort.initializeDefaultProfile(" google-1 ");

        assertThat(memberProfileJpaRepository.findByMember_GoogleSub("google-1"))
                .hasValueSatisfying(profile -> {
                    assertThat(profile.getDisplayName()).isEqualTo("User One");
                    assertThat(profile.getAvatarKind()).isEqualTo(MemberProfileJpaEntity.DEFAULT_AVATAR_KIND);
                });
    }

    @Test
    void initializeDefaultProfileIsIdempotent() {
        memberJpaRepository.save(MemberJpaEntity.create(
                "google-2",
                "user2@example.com",
                "User Two",
                MemberRole.USER));

        initializeMemberProfilePort.initializeDefaultProfile("google-2");
        initializeMemberProfilePort.initializeDefaultProfile("google-2");

        assertThat(memberProfileJpaRepository.findAll()).hasSize(1);
    }
}
