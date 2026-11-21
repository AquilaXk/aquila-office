package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CreateWorkspaceGatewayBindingCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceGatewayBindingResult;
import com.aquilaoffice.application.port.in.CreateWorkspaceGatewayBindingUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.out.AuthenticateWorkspaceGatewayPort;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.WorkspaceGatewayAuthenticationCommand;
import com.aquilaoffice.application.port.out.WorkspaceGatewayAuthenticationResult;
import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * workspace gateway binding 등록 유스케이스를 수행하는 application service다.
 */
public class CreateWorkspaceGatewayBindingService implements CreateWorkspaceGatewayBindingUseCase {

    private static final String EXTERNAL_MODE = "EXTERNAL";

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final SaveWorkspaceGatewayBindingPort saveWorkspaceGatewayBindingPort;
    private final AuthenticateWorkspaceGatewayPort authenticateWorkspaceGatewayPort;

    public CreateWorkspaceGatewayBindingService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            SaveWorkspaceGatewayBindingPort saveWorkspaceGatewayBindingPort,
            AuthenticateWorkspaceGatewayPort authenticateWorkspaceGatewayPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.saveWorkspaceGatewayBindingPort =
                Objects.requireNonNull(saveWorkspaceGatewayBindingPort, "saveWorkspaceGatewayBindingPort must not be null");
        this.authenticateWorkspaceGatewayPort = Objects.requireNonNull(
                authenticateWorkspaceGatewayPort, "authenticateWorkspaceGatewayPort must not be null");
    }

    @Override
    public CreateWorkspaceGatewayBindingResult createWorkspaceGatewayBinding(
            CreateWorkspaceGatewayBindingCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Member requester = loadRequester(command.requesterGoogleSub());
        GetWorkspaceDetailResult workspaceDetail = loadWorkspaceDetail(command.workspaceId(), requester.getGoogleSub());
        requireAdmin(workspaceDetail);

        WorkspaceGatewayBinding binding = command.validateConnection()
                ? createAuthenticatedBinding(command, requester)
                : WorkspaceGatewayBinding.create(
                        command.workspaceId(),
                        command.gatewayUrl(),
                        command.token(),
                        requester.getGoogleSub());
        return toResult(saveWorkspaceGatewayBindingPort.save(binding));
    }

    private WorkspaceGatewayBinding createAuthenticatedBinding(
            CreateWorkspaceGatewayBindingCommand command,
            Member requester) {
        WorkspaceGatewayAuthenticationResult authenticationResult =
                authenticateWorkspaceGatewayPort.authenticate(new WorkspaceGatewayAuthenticationCommand(
                        command.workspaceId(),
                        command.gatewayUrl(),
                        command.token()));
        if (!authenticationResult.connected()) {
            throw new IllegalStateException(
                    "gateway authentication failed: " + resolveFailureMessage(authenticationResult));
        }
        LocalDateTime checkedAt = LocalDateTime.now();
        return WorkspaceGatewayBinding.createAuthenticated(
                command.workspaceId(),
                command.gatewayUrl(),
                command.token(),
                requester.getGoogleSub(),
                authenticationResult.deviceId(),
                authenticationResult.deviceName(),
                checkedAt,
                authenticationResult.status(),
                checkedAt,
                null);
    }

    private Member loadRequester(String googleSub) {
        return loadMemberPort
                .findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
    }

    private GetWorkspaceDetailResult loadWorkspaceDetail(Long workspaceId, String googleSub) {
        return loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace not found"));
    }

    private void requireAdmin(GetWorkspaceDetailResult workspaceDetail) {
        if (workspaceDetail.myRole() != WorkspaceMemberRole.ADMIN) {
            throw new IllegalArgumentException("workspace admin membership required");
        }
    }

    private CreateWorkspaceGatewayBindingResult toResult(WorkspaceGatewayBinding binding) {
        return new CreateWorkspaceGatewayBindingResult(
                binding.getId(),
                binding.getWorkspaceId(),
                EXTERNAL_MODE,
                binding.getGatewayUrl(),
                binding.maskedToken());
    }

    private String resolveFailureMessage(WorkspaceGatewayAuthenticationResult authenticationResult) {
        if (authenticationResult.failureReason() != null && !authenticationResult.failureReason().isBlank()) {
            return authenticationResult.failureReason().trim();
        }
        if (authenticationResult.message() != null && !authenticationResult.message().isBlank()) {
            return authenticationResult.message().trim();
        }
        return authenticationResult.status().name();
    }
}
