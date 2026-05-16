package com.aquilaoffice.application.service;

import com.aquilaoffice.domain.workspace.WorkspacePresenceMessage;

/**
 * application 계층이 구체 SSE 구현에 묶이지 않고 presence event를 내보내기 위한 callback이다.
 */
public interface WorkspacePresenceEventSink {

    void send(WorkspacePresenceMessage message);
}
