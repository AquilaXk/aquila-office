package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;
import java.util.Optional;

/**
 * workspace 단위 active gateway binding 조회 outbound port다.
 */
public interface LoadWorkspaceGatewayBindingPort {

    Optional<WorkspaceGatewayBinding> findActiveByWorkspaceId(Long workspaceId);
}
