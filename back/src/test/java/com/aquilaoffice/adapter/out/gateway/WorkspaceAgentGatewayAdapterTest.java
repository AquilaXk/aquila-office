package com.aquilaoffice.adapter.out.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.out.WorkspaceAgentRunCommand;
import com.aquilaoffice.application.port.out.WorkspaceAgentRunResult;
import org.junit.jupiter.api.Test;

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

        private final WorkspaceAgentGatewayResponse response;
        private WorkspaceAgentGatewayRequest request;

        private RecordingGatewayClient(WorkspaceAgentGatewayResponse response) {
            this.response = response;
        }

        @Override
        public WorkspaceAgentGatewayResponse run(WorkspaceAgentGatewayRequest request) {
            this.request = request;
            return response;
        }

        private WorkspaceAgentGatewayRequest request() {
            return request;
        }
    }
}
