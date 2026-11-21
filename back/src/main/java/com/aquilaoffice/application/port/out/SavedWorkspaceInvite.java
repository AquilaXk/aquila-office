package com.aquilaoffice.application.port.out;

/**
 * 저장소 어댑터가 워크스페이스 초대 저장 후 application 계층에 돌려주는 최소 결과값이다.
 */
public record SavedWorkspaceInvite(Long inviteId) {

    public SavedWorkspaceInvite {
        if (inviteId == null) {
            throw new IllegalArgumentException("inviteId must not be null");
        }
    }
}
