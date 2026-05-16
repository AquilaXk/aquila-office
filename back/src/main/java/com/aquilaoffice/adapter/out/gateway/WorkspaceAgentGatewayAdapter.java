package com.aquilaoffice.adapter.out.gateway;

import com.aquilaoffice.application.port.out.RunWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunCommand;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunResult;
import java.util.Objects;
import org.springframework.stereotype.Repository;

/**
 * Task 실행 요청을 Gateway client boundary로 전달하는 outbound adapter다.
 */
@Repository
public class WorkspaceAgentGatewayAdapter implements RunWorkspaceAgentPort {

    private final WorkspaceAgentGatewayClient gatewayClient;

    public WorkspaceAgentGatewayAdapter() {
        this(new UnconfiguredWorkspaceAgentGatewayClient());
    }

    WorkspaceAgentGatewayAdapter(WorkspaceAgentGatewayClient gatewayClient) {
        this.gatewayClient = Objects.requireNonNull(gatewayClient, "gatewayClient must not be null");
    }

    @Override
    public WorkspaceAgentRunResult run(WorkspaceAgentRunCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        WorkspaceAgentGatewayResponse response = gatewayClient.run(toRequest(command));
        return new WorkspaceAgentRunResult(response.success(), response.finalText(), response.failureReason());
    }

    private WorkspaceAgentGatewayRequest toRequest(WorkspaceAgentRunCommand command) {
        return new WorkspaceAgentGatewayRequest(
                command.workspaceId(),
                command.taskId(),
                command.executionId(),
                command.agentId(),
                command.openClawAgentId(),
                command.workspacePath(),
                command.gatewayUrl(),
                command.gatewayToken(),
                command.sessionKey(),
                command.prompt(),
                command.createPr());
    }
}
