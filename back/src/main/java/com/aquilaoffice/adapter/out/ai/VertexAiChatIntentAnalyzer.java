package com.aquilaoffice.adapter.out.ai;

import com.aquilaoffice.application.port.out.AnalyzeChatIntentCommand;
import com.aquilaoffice.application.port.out.AnalyzeChatIntentPort;
import com.aquilaoffice.application.port.out.ChatIntentAnalysisResult;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.global.config.properties.VertexAiProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Chat 자유 입력을 Vertex AI Gemini로 Task 실행 intent JSON으로 변환한다.
 */
@Repository
public class VertexAiChatIntentAnalyzer implements AnalyzeChatIntentPort {

    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    private static final int MAX_OUTPUT_TOKENS = 512;

    private final VertexAiProperties properties;
    private final VertexAiAccessTokenProvider accessTokenProvider;
    private final VertexAiGenerateContentClient generateContentClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public VertexAiChatIntentAnalyzer(VertexAiProperties properties) {
        this(
                properties,
                new ServiceAccountAccessTokenProvider(properties),
                new JavaHttpGenerateContentClient(),
                new ObjectMapper());
    }

    VertexAiChatIntentAnalyzer(
            VertexAiProperties properties,
            VertexAiAccessTokenProvider accessTokenProvider,
            VertexAiGenerateContentClient generateContentClient,
            ObjectMapper objectMapper) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.accessTokenProvider = Objects.requireNonNull(accessTokenProvider, "accessTokenProvider must not be null");
        this.generateContentClient =
                Objects.requireNonNull(generateContentClient, "generateContentClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public ChatIntentAnalysisResult analyze(AnalyzeChatIntentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (!properties.isConfigured()) {
            return ChatIntentAnalysisResult.empty();
        }
        try {
            String responseBody = generateContentClient.generateContent(
                    properties.generateContentEndpoint(),
                    accessTokenProvider.accessToken(),
                    requestBody(command));
            return parseResponse(responseBody);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ChatIntentAnalysisResult.empty();
        } catch (RuntimeException | IOException exception) {
            return ChatIntentAnalysisResult.empty();
        }
    }

    private String requestBody(AnalyzeChatIntentCommand command) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        var contents = root.putArray("contents");
        ObjectNode userContent = contents.addObject();
        userContent.put("role", "user");
        userContent.putArray("parts").addObject().put("text", prompt(command));
        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("temperature", 0.1);
        generationConfig.put("maxOutputTokens", MAX_OUTPUT_TOKENS);
        generationConfig.put("responseMimeType", "application/json");
        return objectMapper.writeValueAsString(root);
    }

    private String prompt(AnalyzeChatIntentCommand command) {
        return """
                You convert an Aquila Office chat message into one JSON object.
                Return JSON only with fields:
                title: Korean task title, taskType: one of CODE_REVIEW, PR_REVIEW, BUG_FIX,
                FEATURE_IMPLEMENTATION, REFACTORING, TEST_CREATION, DOCUMENTATION, PR_CREATION, OTHER,
                priority: one of LOW, MEDIUM, HIGH, URGENT, createPr: boolean.
                Keep missing or uncertain fields null except createPr false.
                Chat message: %s
                """.formatted(command.message());
    }

    private ChatIntentAnalysisResult parseResponse(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            return ChatIntentAnalysisResult.empty();
        }
        String text = textOrNull(parts.get(0), "text");
        if (text == null) {
            return ChatIntentAnalysisResult.empty();
        }
        JsonNode intent = objectMapper.readTree(extractJsonObject(text));
        ChatIntentAnalysisResult result = new ChatIntentAnalysisResult(
                textOrNull(intent, "title"),
                enumOrNull(TaskType.class, textOrNull(intent, "taskType")),
                enumOrNull(TaskPriority.class, textOrNull(intent, "priority")),
                booleanOrNull(intent, "createPr"));
        if (result.title() == null
                && result.taskType() == null
                && result.priority() == null
                && result.createPr() == null) {
            return ChatIntentAnalysisResult.empty();
        }
        return result;
    }

    private String extractJsonObject(String text) {
        String normalizedText = text.trim();
        int startIndex = normalizedText.indexOf('{');
        int endIndex = normalizedText.lastIndexOf('}');
        if (startIndex < 0 || endIndex < startIndex) {
            throw new IllegalArgumentException("vertex intent response json is missing");
        }
        return normalizedText.substring(startIndex, endIndex + 1);
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

    private static Boolean booleanOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull() || !value.isBoolean()) {
            return null;
        }
        return value.asBoolean();
    }

    private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    static final class ServiceAccountAccessTokenProvider implements VertexAiAccessTokenProvider {
        static final Duration TOKEN_REFRESH_MARGIN = Duration.ofSeconds(60);
        private static final Duration DEFAULT_TOKEN_TTL = Duration.ofMinutes(55);

        private final VertexAiProperties properties;
        private final CredentialLoader credentialLoader;
        private final Clock clock;
        private final Object lock = new Object();

        private RefreshableCredential cachedCredential;
        private String cachedToken;
        private Instant cachedTokenExpiresAt;

        private ServiceAccountAccessTokenProvider(VertexAiProperties properties) {
            this(properties, ServiceAccountAccessTokenProvider::loadGoogleCredential, Clock.systemUTC());
        }

        ServiceAccountAccessTokenProvider(
                VertexAiProperties properties,
                CredentialLoader credentialLoader,
                Clock clock) {
            this.properties = Objects.requireNonNull(properties, "properties must not be null");
            this.credentialLoader = Objects.requireNonNull(credentialLoader, "credentialLoader must not be null");
            this.clock = Objects.requireNonNull(clock, "clock must not be null");
        }

        @Override
        public String accessToken() throws IOException {
            synchronized (lock) {
                Instant now = clock.instant();
                if (cachedToken != null
                        && cachedTokenExpiresAt != null
                        && now.plus(TOKEN_REFRESH_MARGIN).isBefore(cachedTokenExpiresAt)) {
                    return cachedToken;
                }
                if (cachedCredential == null) {
                    cachedCredential = credentialLoader.load(Path.of(properties.credentialsPath()));
                }
                try {
                    cachedCredential.refresh();
                } catch (IOException exception) {
                    cachedToken = null;
                    cachedTokenExpiresAt = null;
                    throw exception;
                }
                String accessToken = cachedCredential.accessToken();
                if (accessToken == null || accessToken.isBlank()) {
                    cachedToken = null;
                    cachedTokenExpiresAt = null;
                    throw new IOException("vertex ai access token is missing");
                }
                Instant expiresAt = cachedCredential.expiresAt();
                cachedToken = accessToken;
                cachedTokenExpiresAt = expiresAt != null ? expiresAt : now.plus(DEFAULT_TOKEN_TTL);
                return cachedToken;
            }
        }

        private static RefreshableCredential loadGoogleCredential(Path credentialsPath) throws IOException {
            try (var inputStream = Files.newInputStream(credentialsPath)) {
                GoogleCredential credential = GoogleCredential.fromStream(inputStream)
                        .createScoped(List.of(CLOUD_PLATFORM_SCOPE));
                return new RefreshableCredential() {
                    @Override
                    public void refresh() throws IOException {
                        if (!credential.refreshToken()) {
                            throw new IOException("vertex ai credential refresh failed");
                        }
                    }

                    @Override
                    public String accessToken() {
                        return credential.getAccessToken();
                    }

                    @Override
                    public Instant expiresAt() {
                        Long expirationMillis = credential.getExpirationTimeMilliseconds();
                        return expirationMillis == null ? null : Instant.ofEpochMilli(expirationMillis);
                    }
                };
            }
        }
    }

    @FunctionalInterface
    interface CredentialLoader {
        RefreshableCredential load(Path credentialsPath) throws IOException;
    }

    interface RefreshableCredential {
        void refresh() throws IOException;

        String accessToken();

        Instant expiresAt();
    }

    static final class JavaHttpGenerateContentClient implements VertexAiGenerateContentClient {
        static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
        static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

        private final HttpClient httpClient;

        JavaHttpGenerateContentClient() {
            this(HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        }

        JavaHttpGenerateContentClient(HttpClient httpClient) {
            this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        }

        @Override
        public String generateContent(URI endpoint, String accessToken, String requestBody)
                throws IOException, InterruptedException {
            HttpRequest request = toHttpRequest(endpoint, accessToken, requestBody);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("vertex ai request failed: " + response.statusCode());
            }
            return response.body();
        }

        HttpRequest toHttpRequest(URI endpoint, String accessToken, String requestBody) {
            return HttpRequest.newBuilder(endpoint)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
        }
    }
}
