package com.aquilaoffice.application.port.out;

/**
 * 저장소 어댑터가 워크스페이스 저장 후 application 계층에 돌려주는 최소 결과값이다.
 */
public record SavedWorkspace(Long workspaceId) {

    public SavedWorkspace {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId must not be null");
        }
    }
}
