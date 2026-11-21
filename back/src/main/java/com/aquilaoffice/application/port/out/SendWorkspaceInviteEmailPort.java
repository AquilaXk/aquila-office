package com.aquilaoffice.application.port.out;

/**
 * 워크스페이스 초대 메일을 외부 메일 시스템으로 발송하는 outbound port다.
 */
public interface SendWorkspaceInviteEmailPort {

    void send(WorkspaceInviteEmailCommand command);

    static SendWorkspaceInviteEmailPort noop() {
        return command -> {};
    }
}
