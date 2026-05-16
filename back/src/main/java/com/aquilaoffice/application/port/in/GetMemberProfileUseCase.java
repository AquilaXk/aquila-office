package com.aquilaoffice.application.port.in;

/**
 * 요청자 자신의 멤버 프로필을 조회하는 inbound port다.
 */
public interface GetMemberProfileUseCase {

    MemberProfileResult getMemberProfile(GetMemberProfileQuery query);
}

