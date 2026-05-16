package com.aquilaoffice.application.port.out;

import com.aquilaoffice.application.port.in.MemberProfileResult;
import java.util.Optional;

/**
 * 멤버 프로필 조회와 기본 프로필 생성을 persistence 구현 밖으로 분리한 outbound port다.
 */
public interface LoadMemberProfilePort {

    Optional<MemberProfileResult> findOrCreateDefaultByGoogleSub(String googleSub);
}

