package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.member.Member;
import java.util.Optional;

/**
 * application 계층이 회원 조회 저장소 구현에 의존하지 않도록 분리한 outbound port다.
 */
public interface LoadMemberPort {

    Optional<Member> findByGoogleSub(String googleSub);
}
