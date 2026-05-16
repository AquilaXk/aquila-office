package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.member.MemberRole;

/**
 * Google 로그인 회원 저장/갱신을 persistence 구현과 분리하는 outbound port다.
 */
public interface SaveGoogleLoginMemberPort {

    SavedGoogleLoginMember saveOrUpdate(String googleSub, String email, String name, MemberRole role);
}
