package com.aquilaoffice.adapter.out.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.global.config.properties.GatewayRpcProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * WebSocket RPC Gateway client가 request id별 pending response와 timeout을 처리하는지 검증한다.
 */
class RpcWorkspaceAgentGatewayClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsAgentRunRequestAndCompletesMatchingResponse() throws Exception {
        RespondingTransport transport = new RespondingTransport(
                objectMapper,
                requestId -> """
                        {"id":"%s","result":{"success":true,"finalText":"done","failureReason":null}}
                        """.formatted(requestId));
        RpcWorkspaceAgentGatewayClient client = new RpcWorkspaceAgentGatewayClient(
                transport,
                objectMapper,
                new GatewayRpcProperties(Duration.ofMillis(200), "/rpc"));

        WorkspaceAgentGatewayResponse response = client.run(request("https://gateway.example.com/base/"));

        assertThat(response.success()).isTrue();
        assertThat(response.finalText()).isEqualTo("done");
        assertThat(response.failureReason()).isNull();
        assertThat(client.pendingRequestCount()).isZero();
        assertThat(transport.connectedUri()).isEqualTo(URI.create("wss://gateway.example.com/base/rpc"));
        assertThat(transport.closed()).isTrue();

        JsonNode message = objectMapper.readTree(transport.sentMessage());
        assertThat(message.path("id").asText()).isNotBlank();
        assertThat(message.path("method").asText()).isEqualTo("agent.run");
        assertThat(message.path("token").asText()).isEqualTo("secret-token");
        JsonNode params = message.path("params");
        assertThat(params.path("workspaceId").asLong()).isEqualTo(101L);
        assertThat(params.path("taskId").asLong()).isEqualTo(301L);
        assertThat(params.path("executionId").asLong()).isEqualTo(701L);
        assertThat(params.path("agentId").asLong()).isEqualTo(501L);
        assertThat(params.path("openClawAgentId").asText()).isEqualTo("oc-agent-1");
        assertThat(params.path("workspacePath").asText()).isEqualTo("/workspace/project");
        assertThat(params.path("sessionKey").asText()).isEqualTo("workspace-101-execution-701");
        assertThat(params.path("prompt").asText()).isEqualTo("테스트를 작성해주세요.");
        assertThat(params.path("createPr").asBoolean()).isTrue();
    }

    @Test
    void returnsFailureWhenGatewaySendsErrorForMatchingRequest() {
        RespondingTransport transport = new RespondingTransport(
                objectMapper,
                requestId -> """
                        {"id":"%s","error":{"message":"agent not connected"}}
                        """.formatted(requestId));
        RpcWorkspaceAgentGatewayClient client = new RpcWorkspaceAgentGatewayClient(
                transport,
                objectMapper,
                new GatewayRpcProperties(Duration.ofMillis(200), "/rpc"));

        WorkspaceAgentGatewayResponse response = client.run(request("ws://gateway.example.com"));

        assertThat(response.success()).isFalse();
        assertThat(response.finalText()).isNull();
        assertThat(response.failureReason()).isEqualTo("agent not connected");
        assertThat(client.pendingRequestCount()).isZero();
        assertThat(transport.closed()).isTrue();
    }

    @Test
    void returnsTimeoutFailureAndClearsPendingRequestWhenResponseIsMissing() {
        SilentTransport transport = new SilentTransport();
        RpcWorkspaceAgentGatewayClient client = new RpcWorkspaceAgentGatewayClient(
                transport,
                objectMapper,
                new GatewayRpcProperties(Duration.ofMillis(25), "/rpc"));

        WorkspaceAgentGatewayResponse response = client.run(request("https://gateway.example.com"));

        assertThat(response.success()).isFalse();
        assertThat(response.finalText()).isNull();
        assertThat(response.failureReason()).isEqualTo("gateway rpc request timed out");
        assertThat(client.pendingRequestCount()).isZero();
        assertThat(transport.closed()).isTrue();
    }

    private static WorkspaceAgentGatewayRequest request(String gatewayUrl) {
        return new WorkspaceAgentGatewayRequest(
                101L,
                301L,
                701L,
                501L,
                "oc-agent-1",
                "/workspace/project",
                gatewayUrl,
                "secret-token",
                "workspace-101-execution-701",
                "테스트를 작성해주세요.",
                true);
    }

    private static final class RespondingTransport implements GatewayWebSocketTransport {

        private final ObjectMapper objectMapper;
        private final Function<String, String> responseFactory;
        private URI connectedUri;
        private String sentMessage;
        private boolean closed;

        private RespondingTransport(ObjectMapper objectMapper, Function<String, String> responseFactory) {
            this.objectMapper = objectMapper;
            this.responseFactory = responseFactory;
        }

        @Override
        public GatewayWebSocketSession connect(URI uri, GatewayWebSocketListener listener) {
            this.connectedUri = uri;
            return new GatewayWebSocketSession() {
                @Override
                public CompletableFuture<Void> sendText(String message) {
                    sentMessage = message;
                    listener.onText(responseFactory.apply(readRequestId(message)));
                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public CompletableFuture<Void> close() {
                    closed = true;
                    return CompletableFuture.completedFuture(null);
                }
            };
        }

        private String readRequestId(String message) {
            try {
                return objectMapper.readTree(message).path("id").asText();
            } catch (Exception exception) {
                throw new IllegalArgumentException("request json is invalid", exception);
            }
        }

        private URI connectedUri() {
            return connectedUri;
        }

        private String sentMessage() {
            return sentMessage;
        }

        private boolean closed() {
            return closed;
        }
    }

    private static final class SilentTransport implements GatewayWebSocketTransport {

        private boolean closed;

        @Override
        public GatewayWebSocketSession connect(URI uri, GatewayWebSocketListener listener) {
            return new GatewayWebSocketSession() {
                @Override
                public CompletableFuture<Void> sendText(String message) {
                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public CompletableFuture<Void> close() {
                    closed = true;
                    return CompletableFuture.completedFuture(null);
                }
            };
        }

        private boolean closed() {
            return closed;
        }
    }
}
