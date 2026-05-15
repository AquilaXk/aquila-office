package com.aquilaoffice.adapter.out.persistence.member;

import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.domain.member.Member;
import java.util.Optional;
import org.springframework.stereotype.Repository;

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
        return memberJpaRepository.findByGoogleSub(googleSub.trim()).map(MemberJpaEntity::toDomain);
    }
}
