package com.aquilaoffice.application.port.out;

/**
 * 로그인 성공 직후 기본 멤버 프로필을 보장하는 outbound port다.
 */
public interface InitializeMemberProfilePort {

    void initializeDefaultProfile(String googleSub);
}
