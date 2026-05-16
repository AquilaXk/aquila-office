package com.aquilaoffice.application.port.in;

import com.aquilaoffice.application.service.WorkspacePresenceEventSink;

/**
 * 워크스페이스 presence stream을 여는 inbound port다.
 */
public interface OpenWorkspacePresenceStreamUseCase {

    OpenWorkspacePresenceStreamResult openStream(
            OpenWorkspacePresenceStreamCommand command,
            WorkspacePresenceEventSink sink);
}
