package com.aquilaoffice.application.port.in;

import java.time.Duration;

/**
 * 워크스페이스 presence stream을 닫거나 비정상 연결을 정리하는 inbound port다.
 */
public interface CloseWorkspacePresenceStreamUseCase {

    void closeStream(CloseWorkspacePresenceStreamCommand command);

    default void pulseHeartbeatsAndPruneStale(Duration staleIdleDuration) {}
}
