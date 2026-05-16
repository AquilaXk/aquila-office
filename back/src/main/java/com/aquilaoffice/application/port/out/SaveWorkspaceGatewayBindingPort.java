package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;

/**
 * workspace gateway binding 저장 outbound port다.
 */
public interface SaveWorkspaceGatewayBindingPort {

    WorkspaceGatewayBinding save(WorkspaceGatewayBinding binding);
}
