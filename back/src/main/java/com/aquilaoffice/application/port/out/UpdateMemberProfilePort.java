package com.aquilaoffice.application.port.out;

import com.aquilaoffice.application.port.in.MemberProfileResult;
import java.util.Optional;

/**
 * 멤버 프로필 변경 저장을 application service와 persistence adapter 사이에서 분리한다.
 */
public interface UpdateMemberProfilePort {

    Optional<MemberProfileResult> update(MemberProfileUpdate update);
}

