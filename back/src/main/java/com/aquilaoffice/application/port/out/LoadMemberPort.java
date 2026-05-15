package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.member.Member;
import java.util.Optional;

public interface LoadMemberPort {

    Optional<Member> findByGoogleSub(String googleSub);
}
