package com.aquilaoffice.adapter.out.gateway;

import com.aquilaoffice.application.port.out.AuthenticateWorkspaceGatewayPort;
import com.aquilaoffice.application.port.out.RunWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunCommand;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunResult;
import com.aquilaoffice.application.port.out.WorkspaceGatewayAuthenticationCommand;
import com.aquilaoffice.application.port.out.WorkspaceGatewayAuthenticationResult;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Task 실행 요청을 Gateway client boundary로 전달하는 outbound adapter다.
 */
@Repository
public class WorkspaceAgentGatewayAdapter implements RunWorkspaceAgentPort, AuthenticateWorkspaceGatewayPort {

    private final WorkspaceAgentGatewayClient gatewayClient;

    @Autowired
    public WorkspaceAgentGatewayAdapter(WorkspaceAgentGatewayClient gatewayClient) {
        this.gatewayClient = Objects.requireNonNull(gatewayClient, "gatewayClient must not be null");
    }

    WorkspaceAgentGatewayAdapter() {
        this(new UnconfiguredWorkspaceAgentGatewayClient());
    }

    @Override
    public WorkspaceAgentRunResult run(WorkspaceAgentRunCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        WorkspaceAgentGatewayResponse response = gatewayClient.run(toRequest(command));
        return new WorkspaceAgentRunResult(response.success(), response.finalText(), response.failureReason());
    }

    @Override
    public WorkspaceGatewayAuthenticationResult authenticate(WorkspaceGatewayAuthenticationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        WorkspaceGatewayAuthenticationResponse response = gatewayClient.authenticate(toAuthenticationRequest(command));
        return new WorkspaceGatewayAuthenticationResult(
                response.status(),
                response.connected(),
                response.gatewayUrl(),
                response.deviceId(),
                response.deviceName(),
                response.message(),
                response.agentCount(),
                response.failureReason());
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

    private WorkspaceGatewayAuthenticationRequest toAuthenticationRequest(
            WorkspaceGatewayAuthenticationCommand command) {
        return new WorkspaceGatewayAuthenticationRequest(
                command.workspaceId(),
                command.gatewayUrl(),
                command.gatewayToken());
    }
}
