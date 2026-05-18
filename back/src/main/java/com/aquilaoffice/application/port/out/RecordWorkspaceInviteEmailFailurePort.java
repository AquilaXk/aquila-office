package com.aquilaoffice.application.port.out;

/**
 * 초대 메일 발송 실패 정보를 저장소에 기록하는 outbound port다.
 */
public interface RecordWorkspaceInviteEmailFailurePort {

    void record(WorkspaceInviteEmailFailure failure);

    static RecordWorkspaceInviteEmailFailurePort noop() {
        return failure -> {};
    }
}
