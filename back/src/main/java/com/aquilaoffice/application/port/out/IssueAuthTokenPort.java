package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.member.MemberRole;

/**
 * 내부 인증 token 발급 구현을 application 계층과 분리하는 outbound port다.
 */
public interface IssueAuthTokenPort {

    IssuedAuthTokens issue(Long memberId, String email, MemberRole role);
}
