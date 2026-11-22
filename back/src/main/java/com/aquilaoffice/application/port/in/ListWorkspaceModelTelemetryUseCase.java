package com.aquilaoffice.application.port.in;

import com.aquilaoffice.application.port.out.WorkspaceModelTelemetryEntry;
import java.util.List;

/**
 * 워크스페이스의 모델별 실행 텔레메트리를 조회하는 inbound use case다.
 */
public interface ListWorkspaceModelTelemetryUseCase {

    List<WorkspaceModelTelemetryEntry> listTelemetry(Long workspaceId, String requesterGoogleSub);
}
