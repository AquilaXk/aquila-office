package com.aquilaoffice.adapter.out.gateway;

import com.aquilaoffice.global.config.properties.GatewayRpcProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Workspace Agent 실행 요청을 Gateway WebSocket RPC `agent.run`으로 전달한다.
 */
@Component
final class RpcWorkspaceAgentGatewayClient implements WorkspaceAgentGatewayClient {

    private static final String METHOD_AGENT_RUN = "agent.run";
    private static final String TIMEOUT_FAILURE_REASON = "gateway rpc request timed out";
    private static final String INTERRUPTED_FAILURE_REASON = "gateway rpc request interrupted";

    private final GatewayWebSocketTransport transport;
    private final ObjectMapper objectMapper;
    private final GatewayRpcProperties properties;
    private final ConcurrentMap<String, CompletableFuture<WorkspaceAgentGatewayResponse>> pendingRequests =
            new ConcurrentHashMap<>();

    @Autowired
    RpcWorkspaceAgentGatewayClient(
            GatewayWebSocketTransport transport,
            GatewayRpcProperties properties) {
        this(transport, new ObjectMapper(), properties);
    }

    RpcWorkspaceAgentGatewayClient(
            GatewayWebSocketTransport transport,
            ObjectMapper objectMapper,
            GatewayRpcProperties properties) {
        this.transport = Objects.requireNonNull(transport, "transport must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public WorkspaceAgentGatewayResponse run(WorkspaceAgentGatewayRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<WorkspaceAgentGatewayResponse> pendingResponse = new CompletableFuture<>();
        pendingRequests.put(requestId, pendingResponse);
        GatewayWebSocketSession session = null;
        try {
            URI rpcUri = GatewayRpcUrlNormalizer.normalize(request.gatewayUrl(), properties.path());
            session = transport.connect(rpcUri, new RpcGatewayListener());
            sendRequest(session, requestId, request);
            return pendingResponse.get(properties.requestTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            return failure(TIMEOUT_FAILURE_REASON);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failure(INTERRUPTED_FAILURE_REASON);
        } catch (ExecutionException exception) {
            return failure(resolveFailureReason(exception.getCause()));
        } catch (RuntimeException exception) {
            return failure(resolveFailureReason(exception));
        } finally {
            pendingRequests.remove(requestId);
            closeQuietly(session);
        }
    }

    int pendingRequestCount() {
        return pendingRequests.size();
    }

    private void sendRequest(
            GatewayWebSocketSession session,
            String requestId,
            WorkspaceAgentGatewayRequest request)
            throws InterruptedException, ExecutionException, TimeoutException {
        session.sendText(toRpcMessage(requestId, request))
                .get(properties.requestTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }

    private String toRpcMessage(String requestId, WorkspaceAgentGatewayRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("id", requestId);
        root.put("method", METHOD_AGENT_RUN);
        root.put("token", request.gatewayToken());
        ObjectNode params = root.putObject("params");
        params.put("workspaceId", request.workspaceId());
        params.put("taskId", request.taskId());
        params.put("executionId", request.executionId());
        params.put("agentId", request.agentId());
        params.put("openClawAgentId", request.openClawAgentId());
        params.put("workspacePath", request.workspacePath());
        params.put("sessionKey", request.sessionKey());
        params.put("prompt", request.prompt());
        params.put("createPr", request.createPr());
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("gateway rpc request serialization failed", exception);
        }
    }

    private void handleTextMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String requestId = textOrNull(root, "id");
            if (requestId == null) {
                return;
            }
            CompletableFuture<WorkspaceAgentGatewayResponse> pendingResponse = pendingRequests.remove(requestId);
            if (pendingResponse == null) {
                return;
            }
            pendingResponse.complete(toResponse(root));
        } catch (JsonProcessingException exception) {
            completePendingExceptionally(exception);
        }
    }

    private WorkspaceAgentGatewayResponse toResponse(JsonNode root) {
        JsonNode error = root.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            return failure(resolveGatewayErrorMessage(error));
        }
        JsonNode result = root.path("result");
        if (result.isMissingNode() || result.isNull()) {
            return failure("gateway rpc response result is missing");
        }
        return new WorkspaceAgentGatewayResponse(
                result.path("success").asBoolean(false),
                textOrNull(result, "finalText"),
                textOrNull(result, "failureReason"));
    }

    private String resolveGatewayErrorMessage(JsonNode error) {
        String message = textOrNull(error, "message");
        if (message == null) {
            return "gateway rpc request failed";
        }
        return message;
    }

    private void completePendingExceptionally(Throwable error) {
        for (Map.Entry<String, CompletableFuture<WorkspaceAgentGatewayResponse>> entry : pendingRequests.entrySet()) {
            if (pendingRequests.remove(entry.getKey(), entry.getValue())) {
                entry.getValue().completeExceptionally(error);
            }
        }
    }

    private void closeQuietly(GatewayWebSocketSession session) {
        if (session == null) {
            return;
        }
        try {
            session.close().get(1, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException ignored) {
            // close 실패는 이미 반환할 RPC 결과가 결정된 뒤의 정리 실패이므로 호출 결과를 바꾸지 않는다.
        }
    }

    private String resolveFailureReason(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "gateway rpc request failed";
        }
        return throwable.getMessage().trim();
    }

    private WorkspaceAgentGatewayResponse failure(String failureReason) {
        return new WorkspaceAgentGatewayResponse(false, null, failureReason);
    }

    private static String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    private final class RpcGatewayListener implements GatewayWebSocketListener {

        @Override
        public void onText(String message) {
            handleTextMessage(message);
        }

        @Override
        public void onError(Throwable error) {
            completePendingExceptionally(error);
        }
    }
}
