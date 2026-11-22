package com.aquilaoffice.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.in.WorkspaceModelConnectionTestResult;
import com.aquilaoffice.application.port.out.WorkspaceModelConnectionTestRequest;
import com.aquilaoffice.application.port.out.WorkspaceModelRunCommand;
import com.aquilaoffice.application.port.out.WorkspaceModelRunResult;
import com.aquilaoffice.domain.model.ModelProviderConnectionStatus;
import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelWireProtocol;
import com.aquilaoffice.global.security.HostCircuitBreakerRegistry;
import com.aquilaoffice.global.security.OutboundDestinationPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpMultiProviderModelAdapterTest {

    private HttpServer server;
    private String baseUrl;
    private HttpMultiProviderModelAdapter adapter;
    private final AtomicReference<String> lastAuthHeader = new AtomicReference<>();
    private final AtomicReference<String> lastApiKeyHeader = new AtomicReference<>();
    private final AtomicReference<String> lastQuery = new AtomicReference<>();
    private final AtomicReference<String> lastRequestBody = new AtomicReference<>();
    private final AtomicInteger statusToReturn = new AtomicInteger(200);
    private final AtomicReference<String> responseBody = new AtomicReference<>("{}");

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handleExchange);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        adapter = new HttpMultiProviderModelAdapter(
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(),
                OutboundDestinationPolicy.permissiveForTesting(),
                new HostCircuitBreakerRegistry(),
                new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void handleExchange(HttpExchange exchange) throws IOException {
        lastAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
        String anthropicKey = exchange.getRequestHeaders().getFirst("x-api-key");
        String googleKey = exchange.getRequestHeaders().getFirst("x-goog-api-key");
        lastApiKeyHeader.set(anthropicKey != null ? anthropicKey : googleKey);
        lastQuery.set(exchange.getRequestURI().getRawQuery());
        lastRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] bytes = responseBody.get().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusToReturn.get(), bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Test
    void runsAndNormalizesUsageAcrossAllFiveWireProtocols() {
        // 1. OpenAI Responses (store: false)
        responseBody.set("""
                {
                  "model": "gpt-5.4",
                  "output_text": "Responses reply",
                  "usage": {"input_tokens": 11, "output_tokens": 7}
                }
                """);
        WorkspaceModelRunResult responsesResult = adapter.run(new WorkspaceModelRunCommand(
                1L, 2L, 3L, 4L, ModelProviderKind.OPENAI, ModelWireProtocol.OPENAI_RESPONSES,
                baseUrl + "/v1", "sk-openai-test-1234", "gpt-5.4", "sys", "hello"));
        assertThat(responsesResult.success()).isTrue();
        assertThat(responsesResult.finalText()).isEqualTo("Responses reply");
        assertThat(responsesResult.promptTokens()).isEqualTo(11);
        assertThat(responsesResult.completionTokens()).isEqualTo(7);
        assertThat(lastRequestBody.get()).contains("\"store\":false");
        assertThat(lastAuthHeader.get()).isEqualTo("Bearer sk-openai-test-1234");

        // 2. OpenAI Chat
        responseBody.set("""
                {
                  "model": "deepseek-chat",
                  "choices": [{"message": {"content": "Chat reply"}}],
                  "usage": {"prompt_tokens": 15, "completion_tokens": 9}
                }
                """);
        WorkspaceModelRunResult chatResult = adapter.run(new WorkspaceModelRunCommand(
                1L, 2L, 3L, 4L, ModelProviderKind.DEEPSEEK, ModelWireProtocol.OPENAI_CHAT,
                baseUrl + "/v1", "sk-ds-test-1234", "deepseek-chat", "sys", "hello"));
        assertThat(chatResult.finalText()).isEqualTo("Chat reply");
        assertThat(chatResult.promptTokens()).isEqualTo(15);
        assertThat(chatResult.completionTokens()).isEqualTo(9);

        // 3. Anthropic Messages (cache tokens summed into promptTokens)
        responseBody.set("""
                {
                  "model": "claude-sonnet-4-20250514",
                  "content": [{"type": "text", "text": "Anthropic reply"}],
                  "usage": {
                    "input_tokens": 10,
                    "cache_read_input_tokens": 20,
                    "cache_creation_input_tokens": 5,
                    "output_tokens": 12
                  }
                }
                """);
        WorkspaceModelRunResult anthropicResult = adapter.run(new WorkspaceModelRunCommand(
                1L, 2L, 3L, 4L, ModelProviderKind.ANTHROPIC, ModelWireProtocol.ANTHROPIC_MESSAGES,
                baseUrl, "sk-ant-api03-test-1234", "claude-sonnet-4-20250514", "sys", "hello"));
        assertThat(anthropicResult.finalText()).isEqualTo("Anthropic reply");
        assertThat(anthropicResult.promptTokens()).isEqualTo(35);
        assertThat(anthropicResult.completionTokens()).isEqualTo(12);
        assertThat(lastApiKeyHeader.get()).isEqualTo("sk-ant-api03-test-1234");

        // 4. Google Gemini (header x-goog-api-key, skips thought parts, sums toolUsePrompt + thoughts tokens)
        responseBody.set("""
                {
                  "modelVersion": "gemini-2.5-flash-001",
                  "candidates": [{
                    "content": {
                      "parts": [
                        {"text": "internal thought", "thought": true},
                        {"text": "Gemini reply"}
                      ]
                    }
                  }],
                  "usageMetadata": {
                    "promptTokenCount": 8,
                    "toolUsePromptTokenCount": 4,
                    "candidatesTokenCount": 6,
                    "thoughtsTokenCount": 3
                  }
                }
                """);
        WorkspaceModelRunResult geminiResult = adapter.run(new WorkspaceModelRunCommand(
                1L, 2L, 3L, 4L, ModelProviderKind.GOOGLE, ModelWireProtocol.GOOGLE_GEMINI,
                baseUrl, "AIzaSyTestGoogleKey12345", "gemini-2.5-flash", "sys", "hello"));
        assertThat(geminiResult.finalText()).isEqualTo("Gemini reply");
        assertThat(geminiResult.resolvedModel()).isEqualTo("gemini-2.5-flash-001");
        assertThat(geminiResult.promptTokens()).isEqualTo(12);
        assertThat(geminiResult.completionTokens()).isEqualTo(9);
        assertThat(lastApiKeyHeader.get()).isEqualTo("AIzaSyTestGoogleKey12345");
        assertThat(lastQuery.get()).isNull();

        // 5. Ollama Native
        responseBody.set("""
                {
                  "model": "llama3.2:latest",
                  "message": {"content": "Ollama reply"},
                  "prompt_eval_count": 21,
                  "eval_count": 13
                }
                """);
        WorkspaceModelRunResult ollamaResult = adapter.run(new WorkspaceModelRunCommand(
                1L, 2L, 3L, 4L, ModelProviderKind.OLLAMA, ModelWireProtocol.OLLAMA_NATIVE,
                baseUrl, null, "llama3.2:latest", "sys", "hello"));
        assertThat(ollamaResult.finalText()).isEqualTo("Ollama reply");
        assertThat(ollamaResult.promptTokens()).isEqualTo(21);
        assertThat(ollamaResult.completionTokens()).isEqualTo(13);
    }

    @Test
    void testsDiscoveryAndCompletionModesAndMapsErrorsWithCircuitBreakerAndSsrf() {
        responseBody.set("""
                {
                  "models": [{"name": "models/gemini-2.5-flash"}, {"name": "models/gemini-2.5-pro"}]
                }
                """);
        WorkspaceModelConnectionTestResult discovery = adapter.testConnection(
                new WorkspaceModelConnectionTestRequest(
                        ModelProviderKind.GOOGLE, ModelWireProtocol.GOOGLE_GEMINI,
                        baseUrl, "AIzaSyTestGoogleKey12345", "gemini-2.5-flash", "models"));
        assertThat(discovery.ok()).isTrue();
        assertThat(discovery.status()).isEqualTo(ModelProviderConnectionStatus.CONNECTED);
        assertThat(discovery.discoveredModels()).containsExactly("gemini-2.5-flash", "gemini-2.5-pro");

        responseBody.set("""
                {
                  "model": "gpt-5.4-mini",
                  "choices": [{"message": {"content": "OK"}}],
                  "usage": {"prompt_tokens": 5, "completion_tokens": 1}
                }
                """);
        WorkspaceModelConnectionTestResult completion = adapter.testConnection(
                new WorkspaceModelConnectionTestRequest(
                        ModelProviderKind.OPENROUTER, ModelWireProtocol.OPENAI_CHAT,
                        baseUrl + "/v1", "sk-or-test-key", "gpt-5.4-mini", "completion"));
        assertThat(completion.ok()).isTrue();
        assertThat(completion.replyPreview()).isEqualTo("OK");

        // 401 -> TOKEN_INVALID (not retryable)
        statusToReturn.set(401);
        WorkspaceModelConnectionTestResult unauthorized = adapter.testConnection(
                new WorkspaceModelConnectionTestRequest(
                        ModelProviderKind.OPENAI, ModelWireProtocol.OPENAI_CHAT,
                        baseUrl + "/v1", "sk-bad", "gpt-5.4", "models"));
        assertThat(unauthorized.ok()).isFalse();
        assertThat(unauthorized.status()).isEqualTo(ModelProviderConnectionStatus.TOKEN_INVALID);

        // 500 with embedded secret triggers secret redaction + opens circuit after 5 failures
        statusToReturn.set(500);
        responseBody.set("upstream rejected Bearer sk-ant-api03-leakedsecret12345");
        for (int i = 0; i < 5; i++) {
            WorkspaceModelRunResult failed = adapter.run(new WorkspaceModelRunCommand(
                    1L, 2L, 3L, 4L, ModelProviderKind.OPENAI, ModelWireProtocol.OPENAI_CHAT,
                    baseUrl + "/v1", "sk-test", "gpt-5.4", null, "hi"));
            assertThat(failed.retryable()).isTrue();
            assertThat(failed.failureReason()).doesNotContain("sk-ant-api03-leakedsecret12345");
        }
        WorkspaceModelRunResult circuitOpen = adapter.run(new WorkspaceModelRunCommand(
                1L, 2L, 3L, 4L, ModelProviderKind.OPENAI, ModelWireProtocol.OPENAI_CHAT,
                baseUrl + "/v1", "sk-test", "gpt-5.4", null, "hi"));
        assertThat(circuitOpen.failureReason()).contains("circuit open");

        // Oversized completion response (> 64 KiB) is bounded fail-closed
        HttpMultiProviderModelAdapter strictAdapter = new HttpMultiProviderModelAdapter(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                OutboundDestinationPolicy.strict(),
                new HostCircuitBreakerRegistry(),
                new ObjectMapper());
        WorkspaceModelRunResult ssrfBlocked = strictAdapter.run(new WorkspaceModelRunCommand(
                1L, 2L, 3L, 4L, ModelProviderKind.OPENAI, ModelWireProtocol.OPENAI_CHAT,
                "http://169.254.169.254/latest", "sk-test", "gpt-5.4", null, "hi"));
        assertThat(ssrfBlocked.success()).isFalse();
        assertThat(ssrfBlocked.retryable()).isFalse();
    }
}
