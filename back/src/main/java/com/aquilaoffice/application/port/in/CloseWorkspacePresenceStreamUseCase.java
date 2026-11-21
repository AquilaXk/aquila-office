package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스 presence stream을 닫는 inbound port다.
 */
public interface CloseWorkspacePresenceStreamUseCase {

    void closeStream(CloseWorkspacePresenceStreamCommand command);
}
