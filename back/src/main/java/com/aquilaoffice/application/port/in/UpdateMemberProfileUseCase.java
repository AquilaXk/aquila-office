package com.aquilaoffice.application.port.in;

/**
 * 요청자 자신의 멤버 프로필을 부분 수정하는 inbound port다.
 */
public interface UpdateMemberProfileUseCase {

    MemberProfileResult updateMemberProfile(UpdateMemberProfileCommand command);
}

