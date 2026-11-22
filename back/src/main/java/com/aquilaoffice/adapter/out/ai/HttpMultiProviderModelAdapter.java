package com.aquilaoffice.adapter.out.ai;

import com.aquilaoffice.application.port.in.WorkspaceModelConnectionTestResult;
import com.aquilaoffice.application.port.out.RunWorkspaceModelPort;
import com.aquilaoffice.application.port.out.TestWorkspaceModelProviderPort;
import com.aquilaoffice.application.port.out.WorkspaceModelConnectionTestRequest;
import com.aquilaoffice.application.port.out.WorkspaceModelRunCommand;
import com.aquilaoffice.application.port.out.WorkspaceModelRunResult;
import com.aquilaoffice.domain.model.ModelProviderConnectionStatus;
import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelWireProtocol;
import com.aquilaoffice.global.security.BoundedHttpBodySubscribers;
import com.aquilaoffice.global.security.HostCircuitBreakerRegistry;
import com.aquilaoffice.global.security.OutboundDestinationPolicy;
import com.aquilaoffice.global.security.SecretRedactor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 다중 AI 공급자 wire protocol(OpenAI Chat/Responses, Anthropic Messages, Gemini API, Ollama Native) 어댑터다.
 * MIT 참조 어댑터의 페이로드 매핑 및 토큰 사용량 정규화를 기반으로 SSRF 차단, 호스트 서킷 브레이커, 바운드 응답 읽기를 적용한다.
 */
@Repository
public class HttpMultiProviderModelAdapter implements RunWorkspaceModelPort, TestWorkspaceModelProviderPort {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration DISCOVERY_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration CONNECTION_TEST_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration TASK_RUN_TIMEOUT = Duration.ofSeconds(60);
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_DISCOVERED_MODELS = 100;
    private static final int PROBE_MAX_OUTPUT_TOKENS = 64;
    private static final int RUN_MAX_OUTPUT_TOKENS = 4096;
    private static final int MAX_PREVIEW_CHARS = 160;
    private static final int MAX_ERROR_CHARS = 320;
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient baseHttpClient;
    private final OutboundDestinationPolicy outboundDestinationPolicy;
    private final HostCircuitBreakerRegistry hostCircuitBreakerRegistry;
    private final ObjectMapper objectMapper;

    @Autowired
    public HttpMultiProviderModelAdapter(
            OutboundDestinationPolicy outboundDestinationPolicy,
            HostCircuitBreakerRegistry hostCircuitBreakerRegistry,
            ObjectProvider<ObjectMapper> objectMapperProvider) {
        this(
                defaultOkHttpClient(),
                outboundDestinationPolicy,
                hostCircuitBreakerRegistry,
                objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    public HttpMultiProviderModelAdapter(
            HttpClient ignoredLegacyClient,
            OutboundDestinationPolicy outboundDestinationPolicy,
            HostCircuitBreakerRegistry hostCircuitBreakerRegistry,
            ObjectMapper objectMapper) {
        this(defaultOkHttpClient(), outboundDestinationPolicy, hostCircuitBreakerRegistry, objectMapper);
    }

    public HttpMultiProviderModelAdapter(
            OkHttpClient baseHttpClient,
            OutboundDestinationPolicy outboundDestinationPolicy,
            HostCircuitBreakerRegistry hostCircuitBreakerRegistry,
            ObjectMapper objectMapper) {
        this.baseHttpClient = Objects.requireNonNull(baseHttpClient, "baseHttpClient must not be null");
        this.outboundDestinationPolicy = Objects.requireNonNull(
                outboundDestinationPolicy, "outboundDestinationPolicy must not be null");
        this.hostCircuitBreakerRegistry = Objects.requireNonNull(
                hostCircuitBreakerRegistry, "hostCircuitBreakerRegistry must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    private static OkHttpClient defaultOkHttpClient() {
        return new OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    @Override
    public WorkspaceModelConnectionTestResult testConnection(WorkspaceModelConnectionTestRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if ("completion".equalsIgnoreCase(request.mode())) {
            return testCompletion(request);
        }
        return testModelDiscovery(request);
    }

    @Override
    public WorkspaceModelRunResult run(WorkspaceModelRunCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (command.modelId() == null || command.modelId().isBlank()) {
            return WorkspaceModelRunResult.fail("modelId is required", null, false);
        }

        URI targetUri = resolveCompletionUri(command.baseUrl(), command.wireProtocol(), command.modelId());
        String payload = buildCompletionBody(
                command.wireProtocol(),
                command.modelId(),
                command.systemPrompt(),
                command.userPrompt(),
                RUN_MAX_OUTPUT_TOKENS);

        HttpExecutionResult execution = executeOutbound(
                command.providerKind(),
                targetUri,
                "POST",
                command.wireProtocol(),
                command.apiKey(),
                payload,
                TASK_RUN_TIMEOUT,
                BoundedHttpBodySubscribers.MODEL_RUN_MAX_BYTES);

        if (!execution.ok()) {
            return WorkspaceModelRunResult.fail(execution.errorMessage(), command.modelId(), execution.retryable());
        }

        try {
            JsonNode root = objectMapper.readTree(execution.body());
            ParsedCompletion parsed = parseCompletionResponse(command.wireProtocol(), command.modelId(), root);
            if (parsed.text() == null || parsed.text().isBlank()) {
                return WorkspaceModelRunResult.fail(
                        "model returned empty completion text",
                        parsed.resolvedModel(),
                        true);
            }
            return WorkspaceModelRunResult.ok(
                    parsed.text(),
                    parsed.resolvedModel(),
                    parsed.promptTokens(),
                    parsed.completionTokens());
        } catch (IOException exception) {
            return WorkspaceModelRunResult.fail(
                    "invalid model response JSON: " + SecretRedactor.redactThrowable(exception, MAX_ERROR_CHARS),
                    command.modelId(),
                    false);
        }
    }

    private WorkspaceModelConnectionTestResult testModelDiscovery(WorkspaceModelConnectionTestRequest request) {
        long startedNanos = System.nanoTime();
        URI targetUri = resolveDiscoveryUri(request.baseUrl(), request.wireProtocol());
        HttpExecutionResult execution = executeOutbound(
                request.providerKind(),
                targetUri,
                "GET",
                request.wireProtocol(),
                request.apiKey(),
                null,
                DISCOVERY_TIMEOUT,
                BoundedHttpBodySubscribers.DISCOVERY_MAX_BYTES);
        long latencyMs = elapsedMillis(startedNanos);

        if (!execution.ok()) {
            return new WorkspaceModelConnectionTestResult(
                    false,
                    execution.status(),
                    latencyMs,
                    List.of(),
                    request.modelId(),
                    null,
                    null,
                    null,
                    execution.errorMessage());
        }

        try {
            JsonNode root = objectMapper.readTree(execution.body());
            List<String> models = parseDiscoveredModels(request.wireProtocol(), root);
            String resolved = !models.isEmpty() ? models.getFirst() : request.modelId();
            return new WorkspaceModelConnectionTestResult(
                    true,
                    ModelProviderConnectionStatus.CONNECTED,
                    latencyMs,
                    models,
                    resolved,
                    null,
                    null,
                    null,
                    "Connected (" + models.size() + " models)");
        } catch (IOException exception) {
            return new WorkspaceModelConnectionTestResult(
                    false,
                    ModelProviderConnectionStatus.FAILED,
                    latencyMs,
                    List.of(),
                    request.modelId(),
                    null,
                    null,
                    null,
                    "Invalid discovery payload: " + SecretRedactor.redactThrowable(exception, MAX_ERROR_CHARS));
        }
    }

    private WorkspaceModelConnectionTestResult testCompletion(WorkspaceModelConnectionTestRequest request) {
        if (request.modelId() == null || request.modelId().isBlank()) {
            return new WorkspaceModelConnectionTestResult(
                    false,
                    ModelProviderConnectionStatus.FAILED,
                    0L,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    "modelId is required for completion test");
        }
        long startedNanos = System.nanoTime();
        URI targetUri = resolveCompletionUri(request.baseUrl(), request.wireProtocol(), request.modelId());
        String payload = buildCompletionBody(
                request.wireProtocol(),
                request.modelId(),
                "You are a connectivity probe.",
                "Reply with exactly: OK",
                PROBE_MAX_OUTPUT_TOKENS);

        HttpExecutionResult execution = executeOutbound(
                request.providerKind(),
                targetUri,
                "POST",
                request.wireProtocol(),
                request.apiKey(),
                payload,
                CONNECTION_TEST_TIMEOUT,
                BoundedHttpBodySubscribers.CONNECTION_TEST_MAX_BYTES);
        long latencyMs = elapsedMillis(startedNanos);

        if (!execution.ok()) {
            return new WorkspaceModelConnectionTestResult(
                    false,
                    execution.status(),
                    latencyMs,
                    List.of(),
                    request.modelId(),
                    null,
                    null,
                    null,
                    execution.errorMessage());
        }

        try {
            JsonNode root = objectMapper.readTree(execution.body());
            ParsedCompletion parsed = parseCompletionResponse(request.wireProtocol(), request.modelId(), root);
            String preview = truncate(parsed.text(), MAX_PREVIEW_CHARS);
            return new WorkspaceModelConnectionTestResult(
                    true,
                    ModelProviderConnectionStatus.CONNECTED,
                    latencyMs,
                    List.of(),
                    parsed.resolvedModel(),
                    preview,
                    parsed.promptTokens(),
                    parsed.completionTokens(),
                    "Connected");
        } catch (IOException exception) {
            return new WorkspaceModelConnectionTestResult(
                    false,
                    ModelProviderConnectionStatus.FAILED,
                    latencyMs,
                    List.of(),
                    request.modelId(),
                    null,
                    null,
                    null,
                    "Invalid completion payload: " + SecretRedactor.redactThrowable(exception, MAX_ERROR_CHARS));
        }
    }

    private HttpExecutionResult executeOutbound(
            ModelProviderKind providerKind,
            URI targetUri,
            String method,
            ModelWireProtocol wireProtocol,
            String apiKey,
            String jsonBody,
            Duration timeout,
            int maxBytes) {
        boolean allowLoopback = providerKind == ModelProviderKind.OLLAMA;
        OutboundDestinationPolicy.ValidatedDestination destination;
        try {
            destination = outboundDestinationPolicy.validateAndResolveUri(targetUri, allowLoopback);
        } catch (IllegalArgumentException exception) {
            return HttpExecutionResult.fail(
                    ModelProviderConnectionStatus.UNREACHABLE,
                    SecretRedactor.redactThrowable(exception, MAX_ERROR_CHARS),
                    false);
        }

        String host = destination.host();
        HostCircuitBreakerRegistry.CircuitLease lease = hostCircuitBreakerRegistry.tryAcquire(host);
        if (!lease.allowed()) {
            return HttpExecutionResult.fail(
                    ModelProviderConnectionStatus.UNREACHABLE,
                    "Outbound circuit open for host: " + host,
                    true);
        }

        OkHttpClient pinnedClient = baseHttpClient.newBuilder()
                .callTimeout(timeout)
                .readTimeout(timeout)
                .writeTimeout(timeout)
                .dns(requestedHost -> OutboundDestinationPolicy.lookupPinnedHost(destination, requestedHost))
                .build();

        try {
            Request.Builder builder = new Request.Builder()
                    .url(destination.uri().toString())
                    .header("Accept", "application/json");
            applyAuthHeaders(builder, wireProtocol, apiKey);
            if ("POST".equals(method)) {
                builder.header("Content-Type", "application/json; charset=utf-8")
                        .post(RequestBody.create(jsonBody == null ? "" : jsonBody, JSON_MEDIA_TYPE));
            } else {
                builder.get();
            }

            try (Response response = pinnedClient.newCall(builder.build()).execute()) {
                String bodyText = readBoundedUtf8(response.body(), maxBytes);
                int code = response.code();
                if (code >= 200 && code < 300) {
                    hostCircuitBreakerRegistry.recordSuccess(host, lease);
                    return HttpExecutionResult.success(bodyText);
                }
                if (code == 401 || code == 403) {
                    hostCircuitBreakerRegistry.recordSuccess(host, lease);
                    return HttpExecutionResult.fail(
                            ModelProviderConnectionStatus.TOKEN_INVALID,
                            "Authentication rejected by provider (HTTP " + code + ")",
                            false);
                }
                if (code == 429) {
                    hostCircuitBreakerRegistry.recordFailure(host, lease);
                    return HttpExecutionResult.fail(
                            ModelProviderConnectionStatus.RATE_LIMITED,
                            "Provider rate limited request (HTTP 429)",
                            true);
                }
                if (code >= 500) {
                    hostCircuitBreakerRegistry.recordFailure(host, lease);
                    return HttpExecutionResult.fail(
                            ModelProviderConnectionStatus.FAILED,
                            buildHttpErrorSummary(code, bodyText),
                            true);
                }
                hostCircuitBreakerRegistry.recordSuccess(host, lease);
                return HttpExecutionResult.fail(
                        ModelProviderConnectionStatus.FAILED,
                        buildHttpErrorSummary(code, bodyText),
                        false);
            }
        } catch (InterruptedIOException exception) {
            if (Thread.currentThread().isInterrupted()) {
                hostCircuitBreakerRegistry.recordFailure(host, lease);
                return HttpExecutionResult.fail(
                        ModelProviderConnectionStatus.FAILED,
                        "Provider request interrupted",
                        false);
            }
            hostCircuitBreakerRegistry.recordFailure(host, lease);
            return HttpExecutionResult.fail(
                    ModelProviderConnectionStatus.TIMEOUT,
                    "Provider request timed out",
                    true);
        } catch (IOException exception) {
            hostCircuitBreakerRegistry.recordFailure(host, lease);
            return HttpExecutionResult.fail(
                    ModelProviderConnectionStatus.UNREACHABLE,
                    "Provider IO failure: " + SecretRedactor.redactThrowable(exception, MAX_ERROR_CHARS),
                    true);
        }
    }

    private static String readBoundedUtf8(ResponseBody body, int maxBytes) throws IOException {
        if (body == null) {
            return "";
        }
        int limit = Math.max(1, maxBytes);
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(limit, 8192));
        try (InputStream input = body.byteStream()) {
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (total + read > limit) {
                    throw new IOException("HTTP response body exceeds maxBytes: " + limit);
                }
                out.write(buffer, 0, read);
                total += read;
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static void applyAuthHeaders(
            Request.Builder builder,
            ModelWireProtocol protocol,
            String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }
        String trimmedKey = apiKey.trim();
        switch (protocol) {
            case ANTHROPIC_MESSAGES -> builder
                    .header("x-api-key", trimmedKey)
                    .header("anthropic-version", ANTHROPIC_VERSION);
            case GOOGLE_GEMINI -> builder.header("x-goog-api-key", trimmedKey);
            case OPENAI_CHAT, OPENAI_RESPONSES, OLLAMA_NATIVE ->
                    builder.header("Authorization", "Bearer " + trimmedKey);
        }
    }

    private static URI resolveDiscoveryUri(String rawBaseUrl, ModelWireProtocol protocol) {
        String base = stripTrailingSlash(rawBaseUrl);
        return switch (protocol) {
            case OPENAI_CHAT, OPENAI_RESPONSES -> URI.create(appendOpenAiPath(base, "/models"));
            case ANTHROPIC_MESSAGES -> URI.create(ensureSingleVersionSuffix(base, "/v1") + "/models");
            case GOOGLE_GEMINI -> URI.create(ensureSingleVersionSuffix(base, "/v1beta") + "/models");
            case OLLAMA_NATIVE -> URI.create(ensureSingleVersionSuffix(base, "/api") + "/tags");
        };
    }

    private static URI resolveCompletionUri(String rawBaseUrl, ModelWireProtocol protocol, String modelId) {
        String base = stripTrailingSlash(rawBaseUrl);
        return switch (protocol) {
            case OPENAI_CHAT -> URI.create(appendOpenAiPath(base, "/chat/completions"));
            case OPENAI_RESPONSES -> URI.create(appendOpenAiPath(base, "/responses"));
            case ANTHROPIC_MESSAGES -> URI.create(ensureSingleVersionSuffix(base, "/v1") + "/messages");
            case GOOGLE_GEMINI -> {
                String encodedModel = URLEncoder.encode(modelId.trim(), StandardCharsets.UTF_8)
                        .replace("+", "%20");
                yield URI.create(ensureSingleVersionSuffix(base, "/v1beta") + "/models/"
                        + encodedModel + ":generateContent");
            }
            case OLLAMA_NATIVE -> URI.create(ensureSingleVersionSuffix(base, "/api") + "/chat");
        };
    }

    private static String appendOpenAiPath(String base, String suffix) {
        URI parsed = URI.create(base);
        String path = parsed.getPath();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return base + "/v1" + suffix;
        }
        if (base.endsWith(suffix)) {
            return base;
        }
        return base + suffix;
    }

    private static String ensureSingleVersionSuffix(String base, String versionSuffix) {
        if (base.endsWith(versionSuffix)) {
            return base;
        }
        return base + versionSuffix;
    }

    private static String stripTrailingSlash(String url) {
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String buildCompletionBody(
            ModelWireProtocol protocol,
            String modelId,
            String systemPrompt,
            String userPrompt,
            int maxOutputTokens) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            switch (protocol) {
                case OPENAI_CHAT -> buildOpenAiChatPayload(root, modelId, systemPrompt, userPrompt, maxOutputTokens);
                case OPENAI_RESPONSES -> buildOpenAiResponsesPayload(
                        root, modelId, systemPrompt, userPrompt, maxOutputTokens);
                case ANTHROPIC_MESSAGES -> buildAnthropicPayload(
                        root, modelId, systemPrompt, userPrompt, maxOutputTokens);
                case GOOGLE_GEMINI -> buildGeminiPayload(root, systemPrompt, userPrompt, maxOutputTokens);
                case OLLAMA_NATIVE -> buildOllamaPayload(root, modelId, systemPrompt, userPrompt);
            }
            return objectMapper.writeValueAsString(root);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to serialize model request", exception);
        }
    }

    private static void buildOpenAiChatPayload(
            ObjectNode root,
            String modelId,
            String systemPrompt,
            String userPrompt,
            int maxOutputTokens) {
        root.put("model", modelId);
        ArrayNode messages = root.putArray("messages");
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.addObject().put("role", "system").put("content", systemPrompt);
        }
        messages.addObject().put("role", "user").put("content", userPrompt);
        root.put("max_tokens", maxOutputTokens);
    }

    private static void buildOpenAiResponsesPayload(
            ObjectNode root,
            String modelId,
            String systemPrompt,
            String userPrompt,
            int maxOutputTokens) {
        root.put("model", modelId);
        root.put("input", userPrompt);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            root.put("instructions", systemPrompt);
        }
        root.put("store", false);
        root.put("max_output_tokens", maxOutputTokens);
    }

    private static void buildAnthropicPayload(
            ObjectNode root,
            String modelId,
            String systemPrompt,
            String userPrompt,
            int maxOutputTokens) {
        root.put("model", modelId);
        root.put("max_tokens", maxOutputTokens);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            root.put("system", systemPrompt);
        }
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "user").put("content", userPrompt);
    }

    private static void buildGeminiPayload(
            ObjectNode root,
            String systemPrompt,
            String userPrompt,
            int maxOutputTokens) {
        ArrayNode contents = root.putArray("contents");
        ObjectNode userContent = contents.addObject();
        userContent.put("role", "user");
        userContent.putArray("parts").addObject().put("text", userPrompt);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ObjectNode sys = root.putObject("systemInstruction");
            sys.putArray("parts").addObject().put("text", systemPrompt);
        }
        root.putObject("generationConfig").put("maxOutputTokens", maxOutputTokens);
    }

    private static void buildOllamaPayload(
            ObjectNode root,
            String modelId,
            String systemPrompt,
            String userPrompt) {
        root.put("model", modelId);
        ArrayNode messages = root.putArray("messages");
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.addObject().put("role", "system").put("content", systemPrompt);
        }
        messages.addObject().put("role", "user").put("content", userPrompt);
        root.put("stream", false);
    }

    private static List<String> parseDiscoveredModels(ModelWireProtocol protocol, JsonNode root) {
        Set<String> models = new LinkedHashSet<>();
        switch (protocol) {
            case OPENAI_CHAT, OPENAI_RESPONSES, ANTHROPIC_MESSAGES -> {
                JsonNode data = root.path("data");
                if (data.isArray()) {
                    for (JsonNode entry : data) {
                        addModelId(models, entry.path("id").asText(null));
                    }
                }
            }
            case GOOGLE_GEMINI -> {
                JsonNode items = root.path("models");
                if (items.isArray()) {
                    for (JsonNode entry : items) {
                        String name = entry.path("name").asText(null);
                        if (name != null && name.startsWith("models/")) {
                            name = name.substring("models/".length());
                        }
                        addModelId(models, name);
                    }
                }
            }
            case OLLAMA_NATIVE -> {
                JsonNode items = root.path("models");
                if (items.isArray()) {
                    for (JsonNode entry : items) {
                        addModelId(models, entry.path("name").asText(null));
                    }
                }
            }
        }
        return List.copyOf(models);
    }

    private static void addModelId(Set<String> models, String id) {
        if (id != null && !id.isBlank() && models.size() < MAX_DISCOVERED_MODELS) {
            models.add(id.trim());
        }
    }

    private static ParsedCompletion parseCompletionResponse(
            ModelWireProtocol protocol,
            String requestedModel,
            JsonNode root) {
        return switch (protocol) {
            case OPENAI_CHAT -> parseOpenAiChat(requestedModel, root);
            case OPENAI_RESPONSES -> parseOpenAiResponses(requestedModel, root);
            case ANTHROPIC_MESSAGES -> parseAnthropicMessages(requestedModel, root);
            case GOOGLE_GEMINI -> parseGemini(requestedModel, root);
            case OLLAMA_NATIVE -> parseOllamaNative(requestedModel, root);
        };
    }

    private static ParsedCompletion parseOpenAiChat(String requestedModel, JsonNode root) {
        JsonNode choice = root.path("choices").path(0);
        JsonNode message = choice.path("message");
        String text = extractOpenAiContentText(message.path("content"));
        if (text.isBlank()) {
            text = choice.path("text").asText("");
        }
        JsonNode usage = root.path("usage");
        Integer prompt = readFirstInt(usage, "prompt_tokens", "input_tokens");
        Integer completion = readFirstInt(usage, "completion_tokens", "output_tokens");
        String resolvedModel = firstNonBlank(root.path("model").asText(null), requestedModel);
        return new ParsedCompletion(text.trim(), resolvedModel, prompt, completion);
    }

    private static String extractOpenAiContentText(JsonNode contentNode) {
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder out = new StringBuilder();
            for (JsonNode part : contentNode) {
                if (part.isTextual()) {
                    out.append(part.asText());
                } else if (part.hasNonNull("text")) {
                    out.append(part.path("text").asText(""));
                }
            }
            return out.toString();
        }
        return "";
    }

    private static ParsedCompletion parseOpenAiResponses(String requestedModel, JsonNode root) {
        String text = root.path("output_text").asText("");
        if (text.isBlank() && root.path("output").isArray()) {
            StringBuilder out = new StringBuilder();
            for (JsonNode item : root.path("output")) {
                for (JsonNode part : item.path("content")) {
                    if (part.hasNonNull("text")) {
                        out.append(part.path("text").asText(""));
                    }
                }
            }
            text = out.toString();
        }
        JsonNode usage = root.path("usage");
        Integer prompt = readFirstInt(usage, "input_tokens", "prompt_tokens");
        Integer completion = readFirstInt(usage, "output_tokens", "completion_tokens");
        String resolvedModel = firstNonBlank(root.path("model").asText(null), requestedModel);
        return new ParsedCompletion(text.trim(), resolvedModel, prompt, completion);
    }

    private static ParsedCompletion parseAnthropicMessages(String requestedModel, JsonNode root) {
        StringBuilder out = new StringBuilder();
        JsonNode content = root.path("content");
        if (content.isArray()) {
            for (JsonNode block : content) {
                if (block.hasNonNull("text")) {
                    out.append(block.path("text").asText(""));
                }
            }
        }
        JsonNode usage = root.path("usage");
        Integer inputTokens = readNullableInt(usage, "input_tokens");
        int cacheRead = readIntOrZero(usage, "cache_read_input_tokens");
        int cacheWrite = readIntOrZero(usage, "cache_creation_input_tokens");
        Integer prompt = inputTokens == null && cacheRead == 0 && cacheWrite == 0
                ? null
                : (inputTokens == null ? 0 : inputTokens) + cacheRead + cacheWrite;
        Integer completion = readNullableInt(usage, "output_tokens");
        String resolvedModel = firstNonBlank(root.path("model").asText(null), requestedModel);
        return new ParsedCompletion(out.toString().trim(), resolvedModel, prompt, completion);
    }

    private static ParsedCompletion parseGemini(String requestedModel, JsonNode root) {
        StringBuilder out = new StringBuilder();
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if (part.path("thought").asBoolean(false)) {
                    continue;
                }
                if (part.hasNonNull("text")) {
                    out.append(part.path("text").asText(""));
                }
            }
        }
        JsonNode usage = root.path("usageMetadata");
        Integer promptBase = readNullableInt(usage, "promptTokenCount");
        int toolUsePrompt = readIntOrZero(usage, "toolUsePromptTokenCount");
        Integer prompt = promptBase == null && toolUsePrompt == 0
                ? null
                : (promptBase == null ? 0 : promptBase) + toolUsePrompt;

        Integer candidates = readNullableInt(usage, "candidatesTokenCount");
        int thoughts = readIntOrZero(usage, "thoughtsTokenCount");
        Integer completion = candidates == null && thoughts == 0
                ? null
                : (candidates == null ? 0 : candidates) + thoughts;

        String resolvedModel = firstNonBlank(root.path("modelVersion").asText(null), requestedModel);
        return new ParsedCompletion(out.toString().trim(), resolvedModel, prompt, completion);
    }

    private static ParsedCompletion parseOllamaNative(String requestedModel, JsonNode root) {
        String text = firstNonBlank(
                root.path("message").path("content").asText(null),
                root.path("response").asText(""));
        Integer prompt = readNullableInt(root, "prompt_eval_count");
        Integer completion = readNullableInt(root, "eval_count");
        String resolvedModel = firstNonBlank(root.path("model").asText(null), requestedModel);
        return new ParsedCompletion(text == null ? "" : text.trim(), resolvedModel, prompt, completion);
    }

    private static Integer readFirstInt(JsonNode node, String primary, String fallback) {
        Integer first = readNullableInt(node, primary);
        return first != null ? first : readNullableInt(node, fallback);
    }

    private static Integer readNullableInt(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isNumber() ? Math.max(0, child.asInt()) : null;
    }

    private static int readIntOrZero(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isNumber() ? Math.max(0, child.asInt()) : 0;
    }

    private static String buildHttpErrorSummary(int statusCode, String rawBody) {
        String sanitized = SecretRedactor.redactText(rawBody);
        String detail = truncate(sanitized, MAX_ERROR_CHARS);
        if (detail == null || detail.isBlank()) {
            return "Provider returned HTTP " + statusCode;
        }
        return "Provider returned HTTP " + statusCode + ": " + detail;
    }

    private static String truncate(String text, int maxChars) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.length() <= maxChars ? trimmed : trimmed.substring(0, maxChars);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private static long elapsedMillis(long startedNanos) {
        return Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos));
    }

    private record HttpExecutionResult(
            boolean ok,
            ModelProviderConnectionStatus status,
            String body,
            String errorMessage,
            boolean retryable) {

        private static HttpExecutionResult success(String body) {
            return new HttpExecutionResult(true, ModelProviderConnectionStatus.CONNECTED, body, null, false);
        }

        private static HttpExecutionResult fail(
                ModelProviderConnectionStatus status,
                String errorMessage,
                boolean retryable) {
            return new HttpExecutionResult(false, status, null, errorMessage, retryable);
        }
    }

    private record ParsedCompletion(
            String text,
            String resolvedModel,
            Integer promptTokens,
            Integer completionTokens) {}
}
