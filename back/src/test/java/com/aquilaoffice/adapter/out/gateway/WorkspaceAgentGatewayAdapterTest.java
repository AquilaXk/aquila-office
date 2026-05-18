package com.aquilaoffice.adapter.out.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentGatewayCommand;
import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentGatewayResult;
import com.aquilaoffice.application.port.out.ListWorkspaceAgentGatewayCommand;
import com.aquilaoffice.application.port.out.ListWorkspaceAgentGatewayResult;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunCommand;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunResult;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Gateway outbound adapter가 application command를 실행 client 요청으로 변환하는지 검증한다.
 */
class WorkspaceAgentGatewayAdapterTest {

    @Test
    void mapsRunCommandToGatewayClientRequest() {
        RecordingGatewayClient gatewayClient =
                new RecordingGatewayClient(new WorkspaceAgentGatewayResponse(true, " 완료 요약 ", null));
        WorkspaceAgentGatewayAdapter adapter = new WorkspaceAgentGatewayAdapter(gatewayClient);

        WorkspaceAgentRunResult result = adapter.run(command());

        assertThat(result.success()).isTrue();
        assertThat(result.finalText()).isEqualTo("완료 요약");
        assertThat(result.failureReason()).isNull();
        assertThat(gatewayClient.request().workspaceId()).isEqualTo(101L);
        assertThat(gatewayClient.request().taskId()).isEqualTo(301L);
        assertThat(gatewayClient.request().executionId()).isEqualTo(701L);
        assertThat(gatewayClient.request().agentId()).isEqualTo(501L);
        assertThat(gatewayClient.request().openClawAgentId()).isEqualTo("oc-agent-1");
        assertThat(gatewayClient.request().workspacePath()).isEqualTo("/workspace/project");
        assertThat(gatewayClient.request().gatewayUrl()).isEqualTo("https://gateway.example.com");
        assertThat(gatewayClient.request().gatewayToken()).isEqualTo("secret-token");
        assertThat(gatewayClient.request().sessionKey()).isEqualTo("workspace-101-execution-701");
        assertThat(gatewayClient.request().prompt()).isEqualTo("테스트를 작성해주세요.");
        assertThat(gatewayClient.request().createPr()).isTrue();
    }

    @Test
    void returnsFailureWhenGatewayClientIsNotConfigured() {
        WorkspaceAgentGatewayAdapter adapter = new WorkspaceAgentGatewayAdapter();

        WorkspaceAgentRunResult result = adapter.run(command());

        assertThat(result.success()).isFalse();
        assertThat(result.finalText()).isNull();
        assertThat(result.failureReason()).isEqualTo("workspace agent gateway client is not configured");
    }

    @Test
    void mapsListCommandToGatewayClientRequest() {
        RecordingGatewayClient gatewayClient = new RecordingGatewayClient(
                new WorkspaceAgentListGatewayResponse(
                        true,
                        List.of(new WorkspaceAgentListGatewayResponse.Agent(
                                "oc-agent-1",
                                "Backend Agent",
                                WorkspaceAgentCategory.BACKEND.name(),
                                "READY")),
                        null));
        WorkspaceAgentGatewayAdapter adapter = new WorkspaceAgentGatewayAdapter(gatewayClient);

        ListWorkspaceAgentGatewayResult result = adapter.list(
                new ListWorkspaceAgentGatewayCommand(
                        101L,
                        "https://gateway.example.com",
                        "secret-token"));

        assertThat(result.success()).isTrue();
        assertThat(result.failureReason()).isNull();
        assertThat(result.agents()).singleElement().satisfies(agent -> {
            assertThat(agent.openClawAgentId()).isEqualTo("oc-agent-1");
            assertThat(agent.name()).isEqualTo("Backend Agent");
            assertThat(agent.category()).isEqualTo(WorkspaceAgentCategory.BACKEND);
            assertThat(agent.status()).isEqualTo("READY");
        });
        assertThat(gatewayClient.listRequest().workspaceId()).isEqualTo(101L);
        assertThat(gatewayClient.listRequest().gatewayUrl()).isEqualTo("https://gateway.example.com");
        assertThat(gatewayClient.listRequest().gatewayToken()).isEqualTo("secret-token");
    }

    @Test
    void mapsDeleteCommandToGatewayClientRequest() {
        RecordingGatewayClient gatewayClient =
                new RecordingGatewayClient(new WorkspaceAgentDeleteGatewayResponse(true, null));
        WorkspaceAgentGatewayAdapter adapter = new WorkspaceAgentGatewayAdapter(gatewayClient);

        DeleteWorkspaceAgentGatewayResult result = adapter.delete(
                new DeleteWorkspaceAgentGatewayCommand(
                        101L,
                        "https://gateway.example.com",
                        "secret-token",
                        "oc-agent-1"));

        assertThat(result.success()).isTrue();
        assertThat(result.failureReason()).isNull();
        assertThat(gatewayClient.deleteRequest().workspaceId()).isEqualTo(101L);
        assertThat(gatewayClient.deleteRequest().gatewayUrl()).isEqualTo("https://gateway.example.com");
        assertThat(gatewayClient.deleteRequest().gatewayToken()).isEqualTo("secret-token");
        assertThat(gatewayClient.deleteRequest().openClawAgentId()).isEqualTo("oc-agent-1");
    }

    @Test
    void gatewayClientConstructorIsAutowiredForSpringWiring() throws Exception {
        Constructor<WorkspaceAgentGatewayAdapter> constructor =
                WorkspaceAgentGatewayAdapter.class.getConstructor(WorkspaceAgentGatewayClient.class);

        assertThat(constructor.isAnnotationPresent(Autowired.class)).isTrue();
    }

    private static WorkspaceAgentRunCommand command() {
        return new WorkspaceAgentRunCommand(
                101L,
                301L,
                701L,
                501L,
                "oc-agent-1",
                "/workspace/project",
                "https://gateway.example.com",
                "secret-token",
                "workspace-101-execution-701",
                "테스트를 작성해주세요.",
                true);
    }

    private static final class RecordingGatewayClient implements WorkspaceAgentGatewayClient {

        private final Object response;
        private WorkspaceAgentGatewayRequest request;
        private WorkspaceAgentListGatewayRequest listRequest;
        private WorkspaceAgentDeleteGatewayRequest deleteRequest;

        private RecordingGatewayClient(Object response) {
            this.response = response;
        }

        @Override
        public WorkspaceAgentCreateGatewayResponse create(WorkspaceAgentCreateGatewayRequest request) {
            throw new UnsupportedOperationException("not used in run mapping test");
        }

        @Override
        public WorkspaceAgentListGatewayResponse list(WorkspaceAgentListGatewayRequest request) {
            this.listRequest = request;
            return (WorkspaceAgentListGatewayResponse) response;
        }

        @Override
        public WorkspaceAgentDeleteGatewayResponse delete(WorkspaceAgentDeleteGatewayRequest request) {
            this.deleteRequest = request;
            return (WorkspaceAgentDeleteGatewayResponse) response;
        }

        @Override
        public WorkspaceAgentGatewayResponse run(WorkspaceAgentGatewayRequest request) {
            this.request = request;
            return (WorkspaceAgentGatewayResponse) response;
        }

        @Override
        public WorkspaceGatewayAuthenticationResponse authenticate(WorkspaceGatewayAuthenticationRequest request) {
            throw new UnsupportedOperationException("not used in run mapping test");
        }

        private WorkspaceAgentGatewayRequest request() {
            return request;
        }

        private WorkspaceAgentListGatewayRequest listRequest() {
            return listRequest;
        }

        private WorkspaceAgentDeleteGatewayRequest deleteRequest() {
            return deleteRequest;
        }
    }
}
