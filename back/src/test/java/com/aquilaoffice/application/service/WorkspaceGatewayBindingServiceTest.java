package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.CreateWorkspaceGatewayBindingCommand;
import com.aquilaoffice.application.port.in.CreateWorkspaceGatewayBindingResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.GetWorkspaceGatewayStatusQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceGatewayStatusResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.TestWorkspaceGatewayConnectionCommand;
import com.aquilaoffice.application.port.in.TestWorkspaceGatewayConnectionResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.AuthenticateWorkspaceGatewayPort;
import com.aquilaoffice.application.port.out.WorkspaceGatewayAuthenticationCommand;
import com.aquilaoffice.application.port.out.WorkspaceGatewayAuthenticationResult;
import com.aquilaoffice.domain.gateway.GatewayBindingStatus;
import com.aquilaoffice.domain.gateway.GatewayConnectionStatus;
import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * workspace gateway binding application service가 requester와 workspace 권한을 확인하는지 검증한다.
 */
class WorkspaceGatewayBindingServiceTest {

    @Test
    void getWorkspaceGatewayStatusReturnsUnboundForJoinedMemberWithoutBinding() {
        GetWorkspaceGatewayStatusService service = new GetWorkspaceGatewayStatusService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                new FakeLoadWorkspaceGatewayBindingPort(Optional.empty()));

        GetWorkspaceGatewayStatusResult result =
                service.getWorkspaceGatewayStatus(new GetWorkspaceGatewayStatusQuery(101L, " google-1 "));

        assertThat(result.status()).isEqualTo(GatewayBindingStatus.UNBOUND);
        assertThat(result.bound()).isFalse();
        assertThat(result.bindingId()).isNull();
        assertThat(result.gatewayUrl()).isNull();
        assertThat(result.maskedToken()).isNull();
    }

    @Test
    void getWorkspaceGatewayStatusReturnsBoundWithMaskedTokenForJoinedMember() {
        FakeLoadWorkspaceGatewayBindingPort bindingPort = new FakeLoadWorkspaceGatewayBindingPort(Optional.of(
                WorkspaceGatewayBinding.restore(
                        501L,
                        101L,
                        "https://gateway.example.com",
                        "gate_1234567890abcdef",
                        "google-1")));
        GetWorkspaceGatewayStatusService service = new GetWorkspaceGatewayStatusService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-2", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                bindingPort);

        GetWorkspaceGatewayStatusResult result =
                service.getWorkspaceGatewayStatus(new GetWorkspaceGatewayStatusQuery(101L, "google-2"));

        assertThat(bindingPort.requestedWorkspaceId).isEqualTo(101L);
        assertThat(result.status()).isEqualTo(GatewayBindingStatus.BOUND);
        assertThat(result.bound()).isTrue();
        assertThat(result.bindingId()).isEqualTo(501L);
        assertThat(result.mode()).isEqualTo("EXTERNAL");
        assertThat(result.gatewayUrl()).isEqualTo("https://gateway.example.com");
        assertThat(result.maskedToken()).isEqualTo("gate****cdef");
    }

    @Test
    void getWorkspaceGatewayStatusReturnsBoundWithDeviceIdentityAndLastStatus() {
        LocalDateTime authenticatedAt = LocalDateTime.of(2026, 5, 18, 10, 30);
        LocalDateTime lastCheckedAt = LocalDateTime.of(2026, 5, 18, 10, 31);
        FakeLoadWorkspaceGatewayBindingPort bindingPort = new FakeLoadWorkspaceGatewayBindingPort(Optional.of(
                WorkspaceGatewayBinding.restore(
                        501L,
                        101L,
                        "https://gateway.example.com",
                        "gate_1234567890abcdef",
                        "google-1",
                        "gw-device-1",
                        "Seoul Office Gateway",
                        authenticatedAt,
                        GatewayConnectionStatus.CONNECTED,
                        lastCheckedAt,
                        null)));
        GetWorkspaceGatewayStatusService service = new GetWorkspaceGatewayStatusService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-2", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                bindingPort);

        GetWorkspaceGatewayStatusResult result =
                service.getWorkspaceGatewayStatus(new GetWorkspaceGatewayStatusQuery(101L, "google-2"));

        assertThat(result.deviceId()).isEqualTo("gw-device-1");
        assertThat(result.deviceName()).isEqualTo("Seoul Office Gateway");
        assertThat(result.authenticatedAt()).isEqualTo("2026-05-18T10:30:00");
        assertThat(result.lastStatus()).isEqualTo(GatewayConnectionStatus.CONNECTED);
        assertThat(result.lastCheckedAt()).isEqualTo("2026-05-18T10:31:00");
        assertThat(result.lastError()).isNull();
    }

    @Test
    void testWorkspaceGatewayConnectionRequiresAdminAndReturnsConnectedForValidInput() {
        FakeAuthenticateWorkspaceGatewayPort authenticatePort =
                new FakeAuthenticateWorkspaceGatewayPort(connectedAuthenticationResult());
        TestWorkspaceGatewayConnectionService service = new TestWorkspaceGatewayConnectionService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                authenticatePort);

        TestWorkspaceGatewayConnectionResult result = service.testWorkspaceGatewayConnection(
                new TestWorkspaceGatewayConnectionCommand(
                        101L,
                        " google-1 ",
                        " https://gateway.example.com ",
                        " gate_1234567890abcdef "));

        assertThat(authenticatePort.command.workspaceId()).isEqualTo(101L);
        assertThat(authenticatePort.command.gatewayUrl()).isEqualTo("https://gateway.example.com");
        assertThat(authenticatePort.command.gatewayToken()).isEqualTo("gate_1234567890abcdef");
        assertThat(result.status()).isEqualTo(GatewayConnectionStatus.CONNECTED);
        assertThat(result.connected()).isTrue();
        assertThat(result.gatewayUrl()).isEqualTo("https://gateway.example.com");
        assertThat(result.message()).isEqualTo("gateway connected");
        assertThat(result.agentCount()).isEqualTo(2);
    }

    @Test
    void testWorkspaceGatewayConnectionRejectsNonAdmin() {
        FakeAuthenticateWorkspaceGatewayPort authenticatePort =
                new FakeAuthenticateWorkspaceGatewayPort(connectedAuthenticationResult());
        TestWorkspaceGatewayConnectionService service = new TestWorkspaceGatewayConnectionService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                authenticatePort);

        assertThatThrownBy(() -> service.testWorkspaceGatewayConnection(
                        new TestWorkspaceGatewayConnectionCommand(
                                101L,
                                "google-1",
                                "https://gateway.example.com",
                                "token")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");
        assertThat(authenticatePort.called).isFalse();
    }

    @Test
    void createWorkspaceGatewayBindingRequiresAdminAndSavesBinding() {
        FakeAuthenticateWorkspaceGatewayPort authenticatePort =
                new FakeAuthenticateWorkspaceGatewayPort(connectedAuthenticationResult());
        FakeSaveWorkspaceGatewayBindingPort savePort = new FakeSaveWorkspaceGatewayBindingPort();
        CreateWorkspaceGatewayBindingService service = new CreateWorkspaceGatewayBindingService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                savePort,
                authenticatePort);

        CreateWorkspaceGatewayBindingResult result = service.createWorkspaceGatewayBinding(
                new CreateWorkspaceGatewayBindingCommand(
                        101L,
                        " google-1 ",
                        " https://gateway.example.com ",
                        " gate_1234567890abcdef ",
                        true));

        assertThat(savePort.savedBinding.getWorkspaceId()).isEqualTo(101L);
        assertThat(savePort.savedBinding.getGatewayUrl()).isEqualTo("https://gateway.example.com");
        assertThat(savePort.savedBinding.getToken()).isEqualTo("gate_1234567890abcdef");
        assertThat(savePort.savedBinding.getCreatedByGoogleSub()).isEqualTo("google-1");
        assertThat(savePort.savedBinding.getDeviceId()).isEqualTo("gw-device-1");
        assertThat(savePort.savedBinding.getDeviceName()).isEqualTo("Seoul Office Gateway");
        assertThat(savePort.savedBinding.getAuthenticatedAt()).isNotNull();
        assertThat(savePort.savedBinding.getLastStatus()).isEqualTo(GatewayConnectionStatus.CONNECTED);
        assertThat(savePort.savedBinding.getLastCheckedAt()).isNotNull();
        assertThat(result.id()).isEqualTo(501L);
        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.mode()).isEqualTo("EXTERNAL");
        assertThat(result.maskedToken()).isEqualTo("gate****cdef");
    }

    @Test
    void createWorkspaceGatewayBindingRejectsFailedAuthenticationBeforeSave() {
        FakeAuthenticateWorkspaceGatewayPort authenticatePort = new FakeAuthenticateWorkspaceGatewayPort(
                new WorkspaceGatewayAuthenticationResult(
                        GatewayConnectionStatus.TOKEN_INVALID,
                        false,
                        "https://gateway.example.com",
                        null,
                        null,
                        "gateway token invalid",
                        0,
                        "gateway token invalid"));
        FakeSaveWorkspaceGatewayBindingPort savePort = new FakeSaveWorkspaceGatewayBindingPort();
        CreateWorkspaceGatewayBindingService service = new CreateWorkspaceGatewayBindingService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "admin@example.com", "Admin"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.ADMIN))),
                savePort,
                authenticatePort);

        assertThatThrownBy(() -> service.createWorkspaceGatewayBinding(
                        new CreateWorkspaceGatewayBindingCommand(
                                101L,
                                "google-1",
                                "https://gateway.example.com",
                                "gate_1234567890abcdef",
                                true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("gateway authentication failed: gateway token invalid");
        assertThat(savePort.called).isFalse();
    }

    @Test
    void createWorkspaceGatewayBindingRejectsNonAdmin() {
        FakeAuthenticateWorkspaceGatewayPort authenticatePort =
                new FakeAuthenticateWorkspaceGatewayPort(connectedAuthenticationResult());
        FakeSaveWorkspaceGatewayBindingPort savePort = new FakeSaveWorkspaceGatewayBindingPort();
        CreateWorkspaceGatewayBindingService service = new CreateWorkspaceGatewayBindingService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "member@example.com", "Member"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail(WorkspaceMemberRole.MEMBER))),
                savePort,
                authenticatePort);

        assertThatThrownBy(() -> service.createWorkspaceGatewayBinding(
                        new CreateWorkspaceGatewayBindingCommand(
                                101L,
                                "google-1",
                                "https://gateway.example.com",
                                "token",
                                false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace admin membership required");
        assertThat(savePort.called).isFalse();
        assertThat(authenticatePort.called).isFalse();
    }

    private static WorkspaceGatewayAuthenticationResult connectedAuthenticationResult() {
        return new WorkspaceGatewayAuthenticationResult(
                GatewayConnectionStatus.CONNECTED,
                true,
                "https://gateway.example.com",
                "gw-device-1",
                "Seoul Office Gateway",
                "gateway connected",
                2,
                null);
    }

    private static GetWorkspaceDetailResult workspaceDetail(WorkspaceMemberRole role) {
        return new GetWorkspaceDetailResult(
                101L,
                "Team",
                "Description",
                "google-1",
                role,
                LocalDateTime.of(2026, 5, 15, 10, 0));
    }

    private static class FakeLoadMemberPort implements LoadMemberPort {
        private final Optional<Member> member;

        private FakeLoadMemberPort(Optional<Member> member) {
            this.member = member;
        }

        @Override
        public Optional<Member> findByGoogleSub(String googleSub) {
            return member;
        }
    }

    private static class FakeLoadWorkspacePort implements LoadWorkspacePort {
        private final Optional<GetWorkspaceDetailResult> detailResult;

        private FakeLoadWorkspacePort(Optional<GetWorkspaceDetailResult> detailResult) {
            this.detailResult = detailResult;
        }

        @Override
        public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            throw new UnsupportedOperationException("not used in gateway service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId,
                String googleSub) {
            return detailResult;
        }
    }

    private static class FakeLoadWorkspaceGatewayBindingPort implements LoadWorkspaceGatewayBindingPort {
        private final Optional<WorkspaceGatewayBinding> binding;
        private Long requestedWorkspaceId;

        private FakeLoadWorkspaceGatewayBindingPort(Optional<WorkspaceGatewayBinding> binding) {
            this.binding = binding;
        }

        @Override
        public Optional<WorkspaceGatewayBinding> findActiveByWorkspaceId(Long workspaceId) {
            this.requestedWorkspaceId = workspaceId;
            return binding;
        }
    }

    private static class FakeSaveWorkspaceGatewayBindingPort implements SaveWorkspaceGatewayBindingPort {
        private WorkspaceGatewayBinding savedBinding;
        private boolean called;

        @Override
        public WorkspaceGatewayBinding save(WorkspaceGatewayBinding binding) {
            this.called = true;
            this.savedBinding = binding;
            return WorkspaceGatewayBinding.restore(
                    501L,
                    binding.getWorkspaceId(),
                    binding.getGatewayUrl(),
                    binding.getToken(),
                    binding.getCreatedByGoogleSub(),
                    binding.getDeviceId(),
                    binding.getDeviceName(),
                    binding.getAuthenticatedAt(),
                    binding.getLastStatus(),
                    binding.getLastCheckedAt(),
                    binding.getLastError());
        }
    }

    private static class FakeAuthenticateWorkspaceGatewayPort implements AuthenticateWorkspaceGatewayPort {
        private final WorkspaceGatewayAuthenticationResult result;
        private WorkspaceGatewayAuthenticationCommand command;
        private boolean called;

        private FakeAuthenticateWorkspaceGatewayPort(WorkspaceGatewayAuthenticationResult result) {
            this.result = result;
        }

        @Override
        public WorkspaceGatewayAuthenticationResult authenticate(WorkspaceGatewayAuthenticationCommand command) {
            this.called = true;
            this.command = command;
            return result;
        }
    }
}
