package com.aquilaoffice.application.port.out;

/**
 * application 계층이 워크스페이스 멤버 역할 변경 저장 방식에 의존하지 않도록 분리한 outbound port다.
 */
public interface ChangeWorkspaceMemberRolePort {

    void changeRole(WorkspaceMemberRoleChange change);
}
