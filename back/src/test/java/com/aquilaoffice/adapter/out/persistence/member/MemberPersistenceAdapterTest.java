package com.aquilaoffice.adapter.out.persistence.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.domain.member.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 회원 persistence adapter가 인증 계층에서 필요한 회원 조회 계약을 지키는지 검증한다.
 */
@DataJpaTest
@Import(MemberPersistenceAdapter.class)
class MemberPersistenceAdapterTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private LoadMemberPort loadMemberPort;

    @Test
    void loadMemberByEmailReturnsDomainMember() {
        memberJpaRepository.save(MemberJpaEntity.create(
                "google-1", "user@example.com", "User", MemberRole.USER));

        assertThat(loadMemberPort.findByEmail(" user@example.com "))
                .hasValueSatisfying(member -> {
                    assertThat(member.getGoogleSub()).isEqualTo("google-1");
                    assertThat(member.getEmail()).isEqualTo("user@example.com");
                    assertThat(member.getName()).isEqualTo("User");
                    assertThat(member.getRole()).isEqualTo(MemberRole.USER);
                });
        assertThat(loadMemberPort.findByEmail("missing@example.com")).isEmpty();
    }
}
