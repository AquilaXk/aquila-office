package com.aquilaoffice.adapter.out.gateway;

import com.aquilaoffice.application.port.out.AuthenticateWorkspaceGatewayPort;
import com.aquilaoffice.application.port.out.CreateWorkspaceAgentGatewayCommand;
import com.aquilaoffice.application.port.out.CreateWorkspaceAgentGatewayPort;
import com.aquilaoffice.application.port.out.CreateWorkspaceAgentGatewayResult;
import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentGatewayCommand;
import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentGatewayPort;
import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentGatewayResult;
import com.aquilaoffice.application.port.out.ListWorkspaceAgentGatewayCommand;
import com.aquilaoffice.application.port.out.ListWorkspaceAgentGatewayPort;
import com.aquilaoffice.application.port.out.ListWorkspaceAgentGatewayResult;
import com.aquilaoffice.application.port.out.RunWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunCommand;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunResult;
import com.aquilaoffice.application.port.out.WorkspaceGatewayAuthenticationCommand;
import com.aquilaoffice.application.port.out.WorkspaceGatewayAuthenticationResult;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Task 실행 요청을 Gateway client boundary로 전달하는 outbound adapter다.
 */
@Repository
public class WorkspaceAgentGatewayAdapter
        implements RunWorkspaceAgentPort,
                AuthenticateWorkspaceGatewayPort,
                CreateWorkspaceAgentGatewayPort,
                ListWorkspaceAgentGatewayPort,
                DeleteWorkspaceAgentGatewayPort {

    private final WorkspaceAgentGatewayClient gatewayClient;

    @Autowired
    public WorkspaceAgentGatewayAdapter(WorkspaceAgentGatewayClient gatewayClient) {
        this.gatewayClient = Objects.requireNonNull(gatewayClient, "gatewayClient must not be null");
    }

    WorkspaceAgentGatewayAdapter() {
        this(new UnconfiguredWorkspaceAgentGatewayClient());
    }

    @Override
    public CreateWorkspaceAgentGatewayResult create(CreateWorkspaceAgentGatewayCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        WorkspaceAgentCreateGatewayResponse response = gatewayClient.create(toCreateRequest(command));
        return new CreateWorkspaceAgentGatewayResult(
                response.success(),
                response.openClawAgentId(),
                response.failureReason(),
                response.skillFiles().stream()
                        .map(skillFile -> new CreateWorkspaceAgentGatewayResult.SkillFileResult(
                                skillFile.fileName(),
                                skillFile.success(),
                                skillFile.failureReason()))
                        .toList());
    }

    @Override
    public ListWorkspaceAgentGatewayResult list(ListWorkspaceAgentGatewayCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        WorkspaceAgentListGatewayResponse response = gatewayClient.list(toListRequest(command));
        return new ListWorkspaceAgentGatewayResult(
                response.success(),
                response.agents().stream()
                        .map(agent -> new ListWorkspaceAgentGatewayResult.Agent(
                                agent.openClawAgentId(),
                                agent.name(),
                                toCategory(agent.category()),
                                agent.status()))
                        .toList(),
                response.failureReason());
    }

    @Override
    public DeleteWorkspaceAgentGatewayResult delete(DeleteWorkspaceAgentGatewayCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        WorkspaceAgentDeleteGatewayResponse response = gatewayClient.delete(toDeleteRequest(command));
        return new DeleteWorkspaceAgentGatewayResult(response.success(), response.failureReason());
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

    private WorkspaceAgentCreateGatewayRequest toCreateRequest(CreateWorkspaceAgentGatewayCommand command) {
        return new WorkspaceAgentCreateGatewayRequest(
                command.workspaceId(),
                command.gatewayUrl(),
                command.gatewayToken(),
                command.name(),
                command.emoji(),
                command.category(),
                command.workspacePath(),
                command.skillFiles().stream()
                        .map(skillFile -> new WorkspaceAgentCreateGatewayRequest.SkillFile(
                                skillFile.fileName(),
                                skillFile.content()))
                        .toList());
    }

    private WorkspaceAgentListGatewayRequest toListRequest(ListWorkspaceAgentGatewayCommand command) {
        return new WorkspaceAgentListGatewayRequest(
                command.workspaceId(),
                command.gatewayUrl(),
                command.gatewayToken());
    }

    private WorkspaceAgentDeleteGatewayRequest toDeleteRequest(DeleteWorkspaceAgentGatewayCommand command) {
        return new WorkspaceAgentDeleteGatewayRequest(
                command.workspaceId(),
                command.gatewayUrl(),
                command.gatewayToken(),
                command.openClawAgentId());
    }

    private WorkspaceGatewayAuthenticationRequest toAuthenticationRequest(
            WorkspaceGatewayAuthenticationCommand command) {
        return new WorkspaceGatewayAuthenticationRequest(
                command.workspaceId(),
                command.gatewayUrl(),
                command.gatewayToken());
    }

    private WorkspaceAgentCategory toCategory(String category) {
        try {
            return WorkspaceAgentCategory.valueOf(category);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
