package com.aquilaoffice.adapter.out.gateway;

import com.aquilaoffice.application.service.WorkflowMetrics;
import com.aquilaoffice.domain.gateway.GatewayConnectionStatus;
import com.aquilaoffice.global.config.properties.GatewayRpcProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Workspace Agent 실행과 Gateway 장치 인증을 Gateway WebSocket RPC로 전달한다.
 */
@Component
final class RpcWorkspaceAgentGatewayClient implements WorkspaceAgentGatewayClient {

    private static final String METHOD_AGENT_FILE_SET = "agents.files.set";
    private static final String METHOD_CHAT_SEND = "chat.send";
    private static final String METHOD_AGENT_CREATE = "agents.create";
    private static final String METHOD_AGENT_LIST = "agents.list";
    private static final String METHOD_AGENT_DELETE = "agents.delete";
    private static final String METHOD_CONNECT = "connect";
    private static final String PROTOCOL_VERSION = "2026-05";
    private static final String OPERATOR_SCOPE = "workspace:operator";
    private static final String TIMEOUT_FAILURE_REASON = "gateway rpc request timed out";
    private static final String INTERRUPTED_FAILURE_REASON = "gateway rpc request interrupted";
    private static final List<ConnectionStatusRule> CONNECTION_STATUS_RULES = List.of(
            new ConnectionStatusRule("token", GatewayConnectionStatus.TOKEN_INVALID),
            new ConnectionStatusRule("timeout", GatewayConnectionStatus.TIMEOUT),
            new ConnectionStatusRule("forbidden", GatewayConnectionStatus.FORBIDDEN),
            new ConnectionStatusRule("pairing", GatewayConnectionStatus.PAIRING_REQUIRED),
            new ConnectionStatusRule("unreachable", GatewayConnectionStatus.UNREACHABLE));

    private final GatewayWebSocketTransport transport;
    private final ObjectMapper objectMapper;
    private final GatewayRpcProperties properties;
    private final WorkflowMetrics workflowMetrics;
    private final Set<RpcGatewayListener> activeSessions = ConcurrentHashMap.newKeySet();

    @Autowired
    RpcWorkspaceAgentGatewayClient(
            GatewayWebSocketTransport transport,
            GatewayRpcProperties properties,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            ObjectProvider<WorkflowMetrics> workflowMetricsProvider) {
        this(
                transport,
                objectMapperProvider.getIfAvailable(ObjectMapper::new),
                properties,
                workflowMetricsProvider.getIfAvailable(WorkflowMetrics::noop));
    }

    RpcWorkspaceAgentGatewayClient(
            GatewayWebSocketTransport transport,
            ObjectMapper objectMapper,
            GatewayRpcProperties properties) {
        this(transport, objectMapper, properties, WorkflowMetrics.noop());
    }

    RpcWorkspaceAgentGatewayClient(
            GatewayWebSocketTransport transport,
            ObjectMapper objectMapper,
            GatewayRpcProperties properties,
            WorkflowMetrics workflowMetrics) {
        this.transport = Objects.requireNonNull(transport, "transport must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.workflowMetrics = Objects.requireNonNull(workflowMetrics, "workflowMetrics must not be null");
    }

    @Override
    public WorkspaceAgentCreateGatewayResponse create(WorkspaceAgentCreateGatewayRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return executeWithSession(
                request.gatewayUrl(),
                (session, listener) -> createAndSyncAgent(session, listener, request),
                this::createFailure);
    }

    @Override
    public WorkspaceAgentSyncSkillFilesGatewayResponse syncSkillFiles(
            WorkspaceAgentSyncSkillFilesGatewayRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (request.skillFiles().isEmpty()) {
            return new WorkspaceAgentSyncSkillFilesGatewayResponse(true, null, List.of());
        }
        return executeWithSession(
                request.gatewayUrl(),
                (session, listener) -> syncAgentSkillFiles(session, listener, request),
                reason -> new WorkspaceAgentSyncSkillFilesGatewayResponse(false, reason, List.of()));
    }

    @Override
    public WorkspaceAgentListGatewayResponse list(WorkspaceAgentListGatewayRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return executeWithSession(
                request.gatewayUrl(),
                (session, listener) -> toAgentListResponse(exchange(
                        session,
                        listener,
                        METHOD_AGENT_LIST,
                        request.gatewayToken(),
                        workspaceParams(request.workspaceId()))),
                this::listFailure);
    }

    @Override
    public WorkspaceAgentDeleteGatewayResponse delete(WorkspaceAgentDeleteGatewayRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return executeWithSession(
                request.gatewayUrl(),
                (session, listener) -> toAgentDeleteResponse(exchange(
                        session,
                        listener,
                        METHOD_AGENT_DELETE,
                        request.gatewayToken(),
                        agentDeleteParams(request))),
                this::deleteFailure);
    }

    @Override
    public WorkspaceAgentGatewayResponse run(WorkspaceAgentGatewayRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return executeWithSession(
                request.gatewayUrl(),
                (session, listener) -> toAgentRunResponse(exchange(
                        session,
                        listener,
                        METHOD_CHAT_SEND,
                        request.gatewayToken(),
                        agentRunParams(request))),
                this::failure);
    }

    @Override
    public WorkspaceGatewayAuthenticationResponse authenticate(WorkspaceGatewayAuthenticationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return executeWithSession(
                request.gatewayUrl(),
                (session, listener) -> toAuthenticationResponse(
                        exchange(
                                session,
                                listener,
                                METHOD_CONNECT,
                                request.gatewayToken(),
                                connectParams(request.workspaceId())),
                        request.gatewayUrl()),
                reason -> authenticationFailure(GatewayConnectionStatus.TIMEOUT, request.gatewayUrl(), reason),
                reason -> authenticationFailure(GatewayConnectionStatus.FAILED, request.gatewayUrl(), reason));
    }

    int pendingRequestCount() {
        int total = 0;
        for (RpcGatewayListener listener : activeSessions) {
            total += listener.pendingCount();
        }
        return total;
    }

    private WorkspaceAgentCreateGatewayResponse createAndSyncAgent(
            GatewayWebSocketSession session,
            RpcGatewayListener listener,
            WorkspaceAgentCreateGatewayRequest request)
            throws InterruptedException, ExecutionException, TimeoutException {
        JsonNode response = exchange(
                session,
                listener,
                METHOD_AGENT_CREATE,
                request.gatewayToken(),
                agentCreateParams(request));
        WorkspaceAgentCreateGatewayResponse createResponse = toAgentCreateResponse(response);
        if (!createResponse.success()) {
            return createResponse;
        }
        List<WorkspaceAgentCreateGatewayResponse.SkillFileResult> skillFileResults = syncSkillFiles(
                session,
                listener,
                request.workspaceId(),
                request.gatewayToken(),
                createResponse.openClawAgentId(),
                request.skillFiles());
        boolean allSkillFilesSynced =
                skillFileResults.stream().allMatch(WorkspaceAgentCreateGatewayResponse.SkillFileResult::success);
        if (!allSkillFilesSynced) {
            return new WorkspaceAgentCreateGatewayResponse(
                    false,
                    createResponse.openClawAgentId(),
                    "gateway skill file sync failed",
                    skillFileResults);
        }
        return new WorkspaceAgentCreateGatewayResponse(
                true,
                createResponse.openClawAgentId(),
                null,
                skillFileResults);
    }

    private WorkspaceAgentSyncSkillFilesGatewayResponse syncAgentSkillFiles(
            GatewayWebSocketSession session,
            RpcGatewayListener listener,
            WorkspaceAgentSyncSkillFilesGatewayRequest request)
            throws InterruptedException, ExecutionException, TimeoutException {
        List<WorkspaceAgentCreateGatewayResponse.SkillFileResult> skillFileResults = syncSkillFiles(
                session,
                listener,
                request.workspaceId(),
                request.gatewayToken(),
                request.openClawAgentId(),
                request.skillFiles());
        boolean allSynced =
                skillFileResults.stream().allMatch(WorkspaceAgentCreateGatewayResponse.SkillFileResult::success);
        return new WorkspaceAgentSyncSkillFilesGatewayResponse(
                allSynced,
                allSynced ? null : "gateway skill file sync failed",
                skillFileResults);
    }

    private <T> T executeWithSession(
            String gatewayUrl,
            RpcSessionAction<T> action,
            Function<String, T> failureMapper) {
        return executeWithSession(gatewayUrl, action, failureMapper, failureMapper);
    }

    private <T> T executeWithSession(
            String gatewayUrl,
            RpcSessionAction<T> action,
            Function<String, T> timeoutFailureMapper,
            Function<String, T> errorFailureMapper) {
        GatewayWebSocketSession session = null;
        RpcGatewayListener listener = new RpcGatewayListener();
        activeSessions.add(listener);
        try {
            URI rpcUri = GatewayRpcUrlNormalizer.normalize(gatewayUrl, properties.path());
            session = transport.connect(rpcUri, listener);
            return action.execute(session, listener);
        } catch (TimeoutException exception) {
            return timeoutFailureMapper.apply(TIMEOUT_FAILURE_REASON);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return errorFailureMapper.apply(INTERRUPTED_FAILURE_REASON);
        } catch (ExecutionException exception) {
            return errorFailureMapper.apply(resolveFailureReason(exception.getCause()));
        } catch (RuntimeException exception) {
            return errorFailureMapper.apply(resolveFailureReason(exception));
        } finally {
            activeSessions.remove(listener);
            listener.clear();
            closeQuietly(session);
        }
    }

    private JsonNode exchange(
            GatewayWebSocketSession session,
            RpcGatewayListener listener,
            String method,
            String token,
            ObjectNode params)
            throws InterruptedException, ExecutionException, TimeoutException {
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<JsonNode> pendingResponse = new CompletableFuture<>();
        WorkflowMetrics.OperationSample metricsSample = workflowMetrics.startGatewayRpc(method);
        listener.registerPending(requestId, pendingResponse);
        try {
            session.sendText(toRpcMessage(requestId, method, token, params))
                    .get(properties.requestTimeout().toMillis(), TimeUnit.MILLISECONDS);
            JsonNode response = pendingResponse.get(properties.requestTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if ("success".equals(resolveRpcOutcome(response))) {
                metricsSample.recordSuccess();
            } else {
                metricsSample.recordFailure();
            }
            return response;
        } catch (TimeoutException exception) {
            metricsSample.recordTimeout();
            throw exception;
        } catch (InterruptedException | ExecutionException | RuntimeException exception) {
            metricsSample.recordFailure();
            throw exception;
        } finally {
            listener.removePending(requestId);
        }
    }

    private ObjectNode agentRunParams(WorkspaceAgentGatewayRequest request) {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("workspaceId", request.workspaceId());
        params.put("taskId", request.taskId());
        params.put("executionId", request.executionId());
        params.put("agentId", request.agentId());
        params.put("openClawAgentId", request.openClawAgentId());
        params.put("workspacePath", request.workspacePath());
        params.put("sessionKey", request.sessionKey());
        params.put("idempotencyKey", "task-" + request.taskId() + "-execution-" + request.executionId());
        params.put("prompt", request.prompt());
        params.put("createPr", request.createPr());
        return params;
    }

    private ObjectNode agentCreateParams(WorkspaceAgentCreateGatewayRequest request) {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("workspaceId", request.workspaceId());
        params.put("name", request.name());
        params.put("category", request.category().name());
        params.put("workspacePath", request.workspacePath());
        if (request.emoji() != null) {
            params.put("emoji", request.emoji());
        }
        return params;
    }

    private List<WorkspaceAgentCreateGatewayResponse.SkillFileResult> syncSkillFiles(
            GatewayWebSocketSession session,
            RpcGatewayListener listener,
            Long workspaceId,
            String gatewayToken,
            String openClawAgentId,
            List<WorkspaceAgentCreateGatewayRequest.SkillFile> skillFiles)
            throws InterruptedException, ExecutionException, TimeoutException {
        List<WorkspaceAgentCreateGatewayResponse.SkillFileResult> results = new ArrayList<>();
        for (WorkspaceAgentCreateGatewayRequest.SkillFile skillFile : skillFiles) {
            JsonNode response = exchange(
                    session,
                    listener,
                    METHOD_AGENT_FILE_SET,
                    gatewayToken,
                    agentFileSetParams(workspaceId, openClawAgentId, skillFile));
            results.add(toSkillFileResult(skillFile.fileName(), response));
        }
        return results;
    }

    private ObjectNode agentFileSetParams(
            Long workspaceId,
            String openClawAgentId,
            WorkspaceAgentCreateGatewayRequest.SkillFile skillFile) {
        ObjectNode params = workspaceParams(workspaceId);
        params.put("openClawAgentId", openClawAgentId);
        params.put("fileName", skillFile.fileName());
        params.put("content", skillFile.content());
        return params;
    }

    private ObjectNode agentDeleteParams(WorkspaceAgentDeleteGatewayRequest request) {
        ObjectNode params = workspaceParams(request.workspaceId());
        params.put("openClawAgentId", request.openClawAgentId());
        return params;
    }

    private ObjectNode workspaceParams(Long workspaceId) {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("workspaceId", workspaceId);
        return params;
    }

    private ObjectNode connectParams(Long workspaceId) {
        ObjectNode params = workspaceParams(workspaceId);
        params.put("protocolVersion", PROTOCOL_VERSION);
        params.put("scope", OPERATOR_SCOPE);
        return params;
    }

    private String toRpcMessage(String requestId, String method, String token, ObjectNode params) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("id", requestId);
        root.put("method", method);
        root.put("token", token);
        root.set("params", params);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("gateway rpc request serialization failed", exception);
        }
    }

    private WorkspaceAgentGatewayResponse toAgentRunResponse(JsonNode root) {
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

    private WorkspaceAgentCreateGatewayResponse toAgentCreateResponse(JsonNode root) {
        JsonNode error = root.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            return createFailure(resolveGatewayErrorMessage(error));
        }
        JsonNode result = root.path("result");
        if (result.isMissingNode() || result.isNull()) {
            return createFailure("gateway agent create result is missing");
        }
        boolean success = result.path("success").asBoolean(false);
        String openClawAgentId = textOrNull(result, "openClawAgentId");
        String failureReason = textOrNull(result, "failureReason");
        if (!success) {
            return createFailure(firstNonBlank(failureReason, "gateway agent create failed"));
        }
        if (openClawAgentId == null) {
            return createFailure("gateway agent id is missing");
        }
        return new WorkspaceAgentCreateGatewayResponse(true, openClawAgentId, null);
    }

    private WorkspaceAgentCreateGatewayResponse.SkillFileResult toSkillFileResult(
            String fileName,
            JsonNode root) {
        JsonNode error = root.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            return new WorkspaceAgentCreateGatewayResponse.SkillFileResult(
                    fileName,
                    false,
                    resolveGatewayErrorMessage(error));
        }
        JsonNode result = root.path("result");
        if (result.isMissingNode() || result.isNull()) {
            return new WorkspaceAgentCreateGatewayResponse.SkillFileResult(
                    fileName,
                    false,
                    "gateway agent file set result is missing");
        }
        boolean success = result.path("success").asBoolean(false);
        return new WorkspaceAgentCreateGatewayResponse.SkillFileResult(
                fileName,
                success,
                success ? null : firstNonBlank(textOrNull(result, "failureReason"), "gateway agent file set failed"));
    }

    private WorkspaceAgentListGatewayResponse toAgentListResponse(JsonNode root) {
        JsonNode error = root.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            return listFailure(resolveGatewayErrorMessage(error));
        }
        JsonNode result = root.path("result");
        if (result.isMissingNode() || result.isNull()) {
            return listFailure("gateway agent list result is missing");
        }
        boolean success = result.path("success").asBoolean(false);
        String failureReason = textOrNull(result, "failureReason");
        if (!success) {
            return listFailure(firstNonBlank(failureReason, "gateway agent list failed"));
        }
        return new WorkspaceAgentListGatewayResponse(true, toAgentList(result.path("agents")), null);
    }

    private WorkspaceAgentDeleteGatewayResponse toAgentDeleteResponse(JsonNode root) {
        JsonNode error = root.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            return deleteFailure(resolveGatewayErrorMessage(error));
        }
        JsonNode result = root.path("result");
        if (result.isMissingNode() || result.isNull()) {
            return deleteFailure("gateway agent delete result is missing");
        }
        boolean success = result.path("success").asBoolean(false);
        String failureReason = textOrNull(result, "failureReason");
        if (!success) {
            return deleteFailure(firstNonBlank(failureReason, "gateway agent delete failed"));
        }
        return new WorkspaceAgentDeleteGatewayResponse(true, null);
    }

    private List<WorkspaceAgentListGatewayResponse.Agent> toAgentList(JsonNode agents) {
        if (!agents.isArray()) {
            return List.of();
        }
        List<WorkspaceAgentListGatewayResponse.Agent> mappedAgents = new ArrayList<>();
        for (JsonNode agent : agents) {
            mappedAgents.add(new WorkspaceAgentListGatewayResponse.Agent(
                    textOrNull(agent, "openClawAgentId"),
                    textOrNull(agent, "name"),
                    textOrNull(agent, "category"),
                    textOrNull(agent, "status")));
        }
        return mappedAgents;
    }

    private WorkspaceGatewayAuthenticationResponse toAuthenticationResponse(
            JsonNode root,
            String gatewayUrl) {
        JsonNode error = root.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            String message = resolveGatewayErrorMessage(error);
            return authenticationFailure(resolveConnectionStatus(error, message), gatewayUrl, message);
        }
        JsonNode result = root.path("result");
        if (result.isMissingNode() || result.isNull()) {
            return authenticationFailure(
                    GatewayConnectionStatus.FAILED,
                    gatewayUrl,
                    "gateway connect result is missing");
        }
        boolean connected = result.path("connected").asBoolean(false);
        String message = textOrNull(result, "message");
        if (!connected) {
            String failureReason = firstNonBlank(textOrNull(result, "failureReason"), message, "gateway connect failed");
            return authenticationFailure(
                    resolveConnectionStatus(result, failureReason),
                    gatewayUrl,
                    failureReason);
        }
        String deviceId = textOrNull(result, "deviceId");
        String deviceName = textOrNull(result, "deviceName");
        if (deviceId == null || deviceName == null) {
            return authenticationFailure(
                    GatewayConnectionStatus.FAILED,
                    gatewayUrl,
                    "gateway device identity is missing");
        }
        return new WorkspaceGatewayAuthenticationResponse(
                GatewayConnectionStatus.CONNECTED,
                true,
                gatewayUrl,
                deviceId,
                deviceName,
                firstNonBlank(message, "gateway connected"),
                result.path("agentCount").asInt(0),
                null);
    }

    private String resolveRpcOutcome(JsonNode root) {
        JsonNode error = root.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            return "failure";
        }
        JsonNode result = root.path("result");
        if (result.isMissingNode() || result.isNull()) {
            return "failure";
        }
        if (result.has("success") && !result.path("success").asBoolean(false)) {
            return "failure";
        }
        if (result.has("connected") && !result.path("connected").asBoolean(false)) {
            return "failure";
        }
        return "success";
    }

    private String resolveGatewayErrorMessage(JsonNode error) {
        String message = textOrNull(error, "message");
        if (message == null) {
            return "gateway rpc request failed";
        }
        return message;
    }

    private GatewayConnectionStatus resolveConnectionStatus(JsonNode node, String message) {
        String code = textOrNull(node, "code");
        String normalizedCode = code == null ? "" : code.toLowerCase(Locale.ROOT);
        String normalizedMessage = message == null ? "" : message.toLowerCase(Locale.ROOT);
        for (ConnectionStatusRule rule : CONNECTION_STATUS_RULES) {
            if (normalizedCode.contains(rule.keyword()) || normalizedMessage.contains(rule.keyword())) {
                return rule.status();
            }
        }
        return GatewayConnectionStatus.FAILED;
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

    private WorkspaceAgentCreateGatewayResponse createFailure(String failureReason) {
        return new WorkspaceAgentCreateGatewayResponse(false, null, failureReason);
    }

    private WorkspaceAgentListGatewayResponse listFailure(String failureReason) {
        return new WorkspaceAgentListGatewayResponse(false, List.of(), failureReason);
    }

    private WorkspaceAgentDeleteGatewayResponse deleteFailure(String failureReason) {
        return new WorkspaceAgentDeleteGatewayResponse(false, failureReason);
    }

    private WorkspaceGatewayAuthenticationResponse authenticationFailure(
            GatewayConnectionStatus status,
            String gatewayUrl,
            String failureReason) {
        return new WorkspaceGatewayAuthenticationResponse(
                status,
                false,
                gatewayUrl,
                null,
                null,
                failureReason,
                0,
                failureReason);
    }

    private static String firstNonBlank(String first, String second) {
        return firstNonBlank(first, second, null);
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return fallback;
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

    @FunctionalInterface
    private interface RpcSessionAction<T> {
        T execute(GatewayWebSocketSession session, RpcGatewayListener listener)
                throws InterruptedException, ExecutionException, TimeoutException;
    }

    private record ConnectionStatusRule(String keyword, GatewayConnectionStatus status) {}

    private final class RpcGatewayListener implements GatewayWebSocketListener {

        private final ConcurrentMap<String, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();

        @Override
        public void onText(String message) {
            try {
                JsonNode root = objectMapper.readTree(message);
                String requestId = textOrNull(root, "id");
                if (requestId == null) {
                    return;
                }
                CompletableFuture<JsonNode> pendingResponse = pendingRequests.remove(requestId);
                if (pendingResponse != null) {
                    pendingResponse.complete(root);
                }
            } catch (JsonProcessingException exception) {
                completePendingExceptionally(exception);
            }
        }

        @Override
        public void onError(Throwable error) {
            completePendingExceptionally(error);
        }

        private void registerPending(String requestId, CompletableFuture<JsonNode> future) {
            pendingRequests.put(requestId, future);
        }

        private void removePending(String requestId) {
            pendingRequests.remove(requestId);
        }

        private int pendingCount() {
            return pendingRequests.size();
        }

        private void clear() {
            completePendingExceptionally(new IllegalStateException("gateway rpc session closed"));
        }

        private void completePendingExceptionally(Throwable error) {
            for (Map.Entry<String, CompletableFuture<JsonNode>> entry : pendingRequests.entrySet()) {
                if (pendingRequests.remove(entry.getKey(), entry.getValue())) {
                    entry.getValue().completeExceptionally(error);
                }
            }
        }
    }
}
