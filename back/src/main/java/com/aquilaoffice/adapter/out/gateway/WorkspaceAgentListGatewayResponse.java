package com.aquilaoffice.adapter.out.gateway;

import java.util.List;

record WorkspaceAgentListGatewayResponse(
        boolean success,
        List<Agent> agents,
        String failureReason) {

    WorkspaceAgentListGatewayResponse {
        agents = agents == null ? List.of() : List.copyOf(agents);
    }

    record Agent(
            String openClawAgentId,
            String name,
            String category,
            String status) {}
}
