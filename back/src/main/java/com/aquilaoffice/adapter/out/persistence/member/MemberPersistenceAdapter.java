package com.aquilaoffice.adapter.out.persistence.member;

import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.domain.member.Member;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 회원 조회 outbound port를 JPA repository로 구현하는 persistence adapter다.
 */
@Repository
public class MemberPersistenceAdapter implements LoadMemberPort {

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
}
