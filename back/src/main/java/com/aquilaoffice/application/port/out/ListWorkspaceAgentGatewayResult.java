package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import java.util.List;

public record ListWorkspaceAgentGatewayResult(
        boolean success,
        List<Agent> agents,
        String failureReason) {

    public ListWorkspaceAgentGatewayResult {
        agents = agents == null ? List.of() : List.copyOf(agents);
    }

    public record Agent(
            String openClawAgentId,
            String name,
            WorkspaceAgentCategory category,
            String status) {}
}
