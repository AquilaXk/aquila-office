package com.aquilaoffice.application.port.out;

/**
 * application 계층이 token 생성 방식에 의존하지 않도록 분리한 outbound port다.
 */
public interface GenerateWorkspaceInviteTokenPort {

    String generateToken();
}
