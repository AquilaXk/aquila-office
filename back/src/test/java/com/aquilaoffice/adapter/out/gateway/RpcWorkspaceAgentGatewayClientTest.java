package com.aquilaoffice.adapter.out.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.global.config.properties.GatewayRpcProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * WebSocket RPC Gateway client가 request id별 pending response와 timeout을 처리하는지 검증한다.
 */
class RpcWorkspaceAgentGatewayClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsChatSendRequestAndCompletesMatchingResponse() throws Exception {
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

        JsonNode message = objectMapper.readTree(transport.sentMessages().getFirst());
        assertThat(message.path("id").asText()).isNotBlank();
        assertThat(message.path("method").asText()).isEqualTo("chat.send");
        assertThat(message.path("token").asText()).isEqualTo("secret-token");
        JsonNode params = message.path("params");
        assertThat(params.path("workspaceId").asLong()).isEqualTo(101L);
        assertThat(params.path("taskId").asLong()).isEqualTo(301L);
        assertThat(params.path("executionId").asLong()).isEqualTo(701L);
        assertThat(params.path("agentId").asLong()).isEqualTo(501L);
        assertThat(params.path("openClawAgentId").asText()).isEqualTo("oc-agent-1");
        assertThat(params.path("workspacePath").asText()).isEqualTo("/workspace/project");
        assertThat(params.path("sessionKey").asText()).isEqualTo("workspace-101-execution-701");
        assertThat(params.path("idempotencyKey").asText()).isEqualTo("task-301-execution-701");
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

    @Test
    void authenticatesGatewayDeviceWithChallengeThenConnect() throws Exception {
        RespondingTransport transport = new RespondingTransport(
                objectMapper,
                (requestId, message) -> {
                    JsonNode request = readJson(message);
                    return """
                            {"id":"%s","result":{
                              "connected":true,
                              "deviceId":"gw-device-1",
                              "deviceName":"Seoul Office Gateway",
                              "message":"gateway connected",
                              "agentCount":2
                            }}
                            """.formatted(requestId);
                });
        RpcWorkspaceAgentGatewayClient client = new RpcWorkspaceAgentGatewayClient(
                transport,
                objectMapper,
                new GatewayRpcProperties(Duration.ofMillis(200), "/rpc"));

        WorkspaceGatewayAuthenticationResponse response = client.authenticate(
                new WorkspaceGatewayAuthenticationRequest(
                        101L,
                        "https://gateway.example.com/base/",
                        "secret-token"));

        assertThat(response.connected()).isTrue();
        assertThat(response.gatewayUrl()).isEqualTo("https://gateway.example.com/base/");
        assertThat(response.deviceId()).isEqualTo("gw-device-1");
        assertThat(response.deviceName()).isEqualTo("Seoul Office Gateway");
        assertThat(response.message()).isEqualTo("gateway connected");
        assertThat(response.agentCount()).isEqualTo(2);
        assertThat(response.failureReason()).isNull();
        assertThat(client.pendingRequestCount()).isZero();
        assertThat(transport.connectedUri()).isEqualTo(URI.create("wss://gateway.example.com/base/rpc"));
        assertThat(transport.closed()).isTrue();

        assertThat(transport.sentMessages()).hasSize(1);
        JsonNode connectMessage = objectMapper.readTree(transport.sentMessages().getFirst());
        assertThat(connectMessage.path("method").asText()).isEqualTo("connect");
        assertThat(connectMessage.path("token").asText()).isEqualTo("secret-token");
        assertThat(connectMessage.path("params").path("workspaceId").asLong()).isEqualTo(101L);
        assertThat(connectMessage.path("params").path("protocolVersion").asText()).isEqualTo("2026-05");
        assertThat(connectMessage.path("params").path("scope").asText()).isEqualTo("workspace:operator");
    }

    @Test
    void sendsAgentListRequestAndMapsGatewayAgents() throws Exception {
        RespondingTransport transport = new RespondingTransport(
                objectMapper,
                requestId -> """
                        {"id":"%s","result":{"success":true,"agents":[
                          {"openClawAgentId":"oc-agent-1","name":"Backend Agent","category":"BACKEND","status":"READY"}
                        ],"failureReason":null}}
                        """.formatted(requestId));
        RpcWorkspaceAgentGatewayClient client = new RpcWorkspaceAgentGatewayClient(
                transport,
                objectMapper,
                new GatewayRpcProperties(Duration.ofMillis(200), "/rpc"));

        WorkspaceAgentListGatewayResponse response = client.list(
                new WorkspaceAgentListGatewayRequest(
                        101L,
                        "https://gateway.example.com/base/",
                        "secret-token"));

        assertThat(response.success()).isTrue();
        assertThat(response.failureReason()).isNull();
        assertThat(response.agents()).singleElement().satisfies(agent -> {
            assertThat(agent.openClawAgentId()).isEqualTo("oc-agent-1");
            assertThat(agent.name()).isEqualTo("Backend Agent");
            assertThat(agent.category()).isEqualTo("BACKEND");
            assertThat(agent.status()).isEqualTo("READY");
        });
        assertThat(client.pendingRequestCount()).isZero();
        assertThat(transport.connectedUri()).isEqualTo(URI.create("wss://gateway.example.com/base/rpc"));
        assertThat(transport.closed()).isTrue();

        JsonNode message = objectMapper.readTree(transport.sentMessage());
        assertThat(message.path("method").asText()).isEqualTo("agents.list");
        assertThat(message.path("token").asText()).isEqualTo("secret-token");
        assertThat(message.path("params").path("workspaceId").asLong()).isEqualTo(101L);
    }

    @Test
    void sendsAgentCreateRequestWithCompatibleMethodName() throws Exception {
        RespondingTransport transport = new RespondingTransport(
                objectMapper,
                requestId -> """
                        {"id":"%s","result":{"success":true,"openClawAgentId":"oc-agent-1","failureReason":null}}
                        """.formatted(requestId));
        RpcWorkspaceAgentGatewayClient client = new RpcWorkspaceAgentGatewayClient(
                transport,
                objectMapper,
                new GatewayRpcProperties(Duration.ofMillis(200), "/rpc"));

        WorkspaceAgentCreateGatewayResponse response = client.create(new WorkspaceAgentCreateGatewayRequest(
                101L,
                "https://gateway.example.com/base/",
                "secret-token",
                "Backend Agent",
                com.aquilaoffice.domain.agent.WorkspaceAgentCategory.BACKEND,
                "/workspace/project",
                List.of(new WorkspaceAgentCreateGatewayRequest.SkillFile("AGENTS.md", "rules"))));

        assertThat(response.success()).isTrue();
        assertThat(response.openClawAgentId()).isEqualTo("oc-agent-1");

        JsonNode message = objectMapper.readTree(transport.sentMessages().getFirst());
        assertThat(message.path("method").asText()).isEqualTo("agents.create");
        assertThat(message.path("params").has("skillFiles")).isFalse();
    }

    @Test
    void sendsAgentDeleteRequestAndMapsGatewayResult() throws Exception {
        RespondingTransport transport = new RespondingTransport(
                objectMapper,
                requestId -> """
                        {"id":"%s","result":{"success":true,"failureReason":null}}
                        """.formatted(requestId));
        RpcWorkspaceAgentGatewayClient client = new RpcWorkspaceAgentGatewayClient(
                transport,
                objectMapper,
                new GatewayRpcProperties(Duration.ofMillis(200), "/rpc"));

        WorkspaceAgentDeleteGatewayResponse response = client.delete(
                new WorkspaceAgentDeleteGatewayRequest(
                        101L,
                        "https://gateway.example.com/base/",
                        "secret-token",
                        "oc-agent-1"));

        assertThat(response.success()).isTrue();
        assertThat(response.failureReason()).isNull();
        assertThat(client.pendingRequestCount()).isZero();
        assertThat(transport.closed()).isTrue();

        JsonNode message = objectMapper.readTree(transport.sentMessage());
        assertThat(message.path("method").asText()).isEqualTo("agents.delete");
        assertThat(message.path("token").asText()).isEqualTo("secret-token");
        JsonNode params = message.path("params");
        assertThat(params.path("workspaceId").asLong()).isEqualTo(101L);
        assertThat(params.path("openClawAgentId").asText()).isEqualTo("oc-agent-1");
    }

    @Test
    void createAgentSendsAgentCreateThenFileSetForEachSkillFile() throws Exception {
        RespondingTransport transport = new RespondingTransport(
                objectMapper,
                (requestId, message) -> {
                    JsonNode request = readJson(message);
                    String method = request.path("method").asText();
                    if ("agents.create".equals(method)) {
                        return """
                                {"id":"%s","result":{"success":true,"openClawAgentId":"oc-agent-1","failureReason":null}}
                                """.formatted(requestId);
                    }
                    return """
                            {"id":"%s","result":{"success":true,"failureReason":null}}
                            """.formatted(requestId);
                });
        RpcWorkspaceAgentGatewayClient client = new RpcWorkspaceAgentGatewayClient(
                transport,
                objectMapper,
                new GatewayRpcProperties(Duration.ofMillis(200), "/rpc"));

        WorkspaceAgentCreateGatewayResponse response = client.create(new WorkspaceAgentCreateGatewayRequest(
                101L,
                "https://gateway.example.com/base/",
                "secret-token",
                "Backend Agent",
                com.aquilaoffice.domain.agent.WorkspaceAgentCategory.BACKEND,
                "/workspace/project",
                List.of(
                        new WorkspaceAgentCreateGatewayRequest.SkillFile("AGENTS.md", "rules"),
                        new WorkspaceAgentCreateGatewayRequest.SkillFile("backend-guide.md", "guide"))));

        assertThat(response.success()).isTrue();
        assertThat(response.openClawAgentId()).isEqualTo("oc-agent-1");
        assertThat(response.skillFiles())
                .extracting(WorkspaceAgentCreateGatewayResponse.SkillFileResult::fileName)
                .containsExactly("AGENTS.md", "backend-guide.md");
        assertThat(transport.sentMessages()).hasSize(3);
        JsonNode createMessage = objectMapper.readTree(transport.sentMessages().get(0));
        assertThat(createMessage.path("method").asText()).isEqualTo("agents.create");
        assertThat(createMessage.path("params").has("skillFiles")).isFalse();
        JsonNode firstFileMessage = objectMapper.readTree(transport.sentMessages().get(1));
        assertThat(firstFileMessage.path("method").asText()).isEqualTo("agents.files.set");
        assertThat(firstFileMessage.path("params").path("openClawAgentId").asText()).isEqualTo("oc-agent-1");
        assertThat(firstFileMessage.path("params").path("fileName").asText()).isEqualTo("AGENTS.md");
        assertThat(firstFileMessage.path("params").path("content").asText()).isEqualTo("rules");
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

    private JsonNode readJson(String message) {
        try {
            return objectMapper.readTree(message);
        } catch (Exception exception) {
            throw new IllegalArgumentException("request json is invalid", exception);
        }
    }

    private static final class RespondingTransport implements GatewayWebSocketTransport {

        private final ObjectMapper objectMapper;
        private final BiFunction<String, String, String> responseFactory;
        private URI connectedUri;
        private final List<String> sentMessages = new ArrayList<>();
        private boolean closed;

        private RespondingTransport(ObjectMapper objectMapper, Function<String, String> responseFactory) {
            this(objectMapper, (requestId, message) -> responseFactory.apply(requestId));
        }

        private RespondingTransport(ObjectMapper objectMapper, BiFunction<String, String, String> responseFactory) {
            this.objectMapper = objectMapper;
            this.responseFactory = responseFactory;
        }

        @Override
        public GatewayWebSocketSession connect(URI uri, GatewayWebSocketListener listener) {
            this.connectedUri = uri;
            return new GatewayWebSocketSession() {
                @Override
                public CompletableFuture<Void> sendText(String message) {
                    sentMessages.add(message);
                    listener.onText(responseFactory.apply(readRequestId(message), message));
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
            return sentMessages.getLast();
        }

        private List<String> sentMessages() {
            return sentMessages;
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
