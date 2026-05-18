package com.aquilaoffice.adapter.out.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.out.AnalyzeChatIntentCommand;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.global.config.properties.VertexAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import org.junit.jupiter.api.Test;

class VertexAiChatIntentAnalyzerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsGenerateContentRequestToConfiguredVertexEndpoint() throws Exception {
        RecordingGenerateContentClient client = new RecordingGenerateContentClient(responseText("""
                {"title":"테스트 자동화","taskType":"TEST_CREATION","priority":"URGENT","createPr":true}
                """));
        VertexAiChatIntentAnalyzer analyzer = new VertexAiChatIntentAnalyzer(
                properties("/tmp/vertex-key.json"),
                () -> "access-token",
                client,
                objectMapper);

        var result = analyzer.analyze(command());

        assertThat(result.title()).isEqualTo("테스트 자동화");
        assertThat(client.endpoint).isEqualTo(URI.create(
                "https://us-central1-aiplatform.googleapis.com/v1/projects/grepp-ai-project"
                        + "/locations/us-central1/publishers/google/models/gemini-2.5-flash:generateContent"));
        assertThat(client.accessToken).isEqualTo("access-token");
        var request = objectMapper.readTree(client.requestBody);
        String prompt = request.path("contents").get(0).path("parts").get(0).path("text").asText();
        assertThat(prompt).contains("긴급하게 테스트를 만들고 PR까지 올려줘");
        assertThat(prompt).contains("JSON");
    }

    @Test
    void parsesIntentJsonFromGeminiResponseText() {
        VertexAiChatIntentAnalyzer analyzer = new VertexAiChatIntentAnalyzer(
                properties("/tmp/vertex-key.json"),
                () -> "access-token",
                new RecordingGenerateContentClient(responseText("""
                        {"title":"리팩터링","taskType":"REFACTORING","priority":"HIGH","createPr":false}
                        """)),
                objectMapper);

        var result = analyzer.analyze(command());

        assertThat(result.title()).isEqualTo("리팩터링");
        assertThat(result.taskType()).isEqualTo(TaskType.REFACTORING);
        assertThat(result.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(result.createPr()).isFalse();
    }

    @Test
    void returnsEmptyIntentWhenConfigurationOrResponseIsInvalid() {
        RecordingGenerateContentClient client = new RecordingGenerateContentClient(responseText("{}"));
        VertexAiChatIntentAnalyzer missingCredentialsAnalyzer = new VertexAiChatIntentAnalyzer(
                properties(""),
                () -> "access-token",
                client,
                objectMapper);
        VertexAiChatIntentAnalyzer malformedResponseAnalyzer = new VertexAiChatIntentAnalyzer(
                properties("/tmp/vertex-key.json"),
                () -> "access-token",
                new RecordingGenerateContentClient("{\"candidates\":[]}"),
                objectMapper);

        var missingCredentialsResult = missingCredentialsAnalyzer.analyze(command());
        var malformedResponseResult = malformedResponseAnalyzer.analyze(command());

        assertThat(missingCredentialsResult).isEqualTo(com.aquilaoffice.application.port.out.ChatIntentAnalysisResult.empty());
        assertThat(client.called).isFalse();
        assertThat(malformedResponseResult).isEqualTo(com.aquilaoffice.application.port.out.ChatIntentAnalysisResult.empty());
    }

    private static AnalyzeChatIntentCommand command() {
        return new AnalyzeChatIntentCommand(
                101L,
                "긴급하게 테스트를 만들고 PR까지 올려줘",
                501L,
                null,
                null,
                null,
                null,
                null);
    }

    private static VertexAiProperties properties(String credentialsPath) {
        return new VertexAiProperties(
                "grepp-ai-project",
                "us-central1",
                "gemini-2.5-flash",
                credentialsPath);
    }

    private static String responseText(String text) {
        return """
                {"candidates":[{"content":{"parts":[{"text":%s}]}}]}
                """.formatted(toJsonString(text));
    }

    private static String toJsonString(String value) {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class RecordingGenerateContentClient implements VertexAiGenerateContentClient {
        private final String responseBody;
        private URI endpoint;
        private String accessToken;
        private String requestBody;
        private boolean called;

        private RecordingGenerateContentClient(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public String generateContent(URI endpoint, String accessToken, String requestBody) {
            this.called = true;
            this.endpoint = endpoint;
            this.accessToken = accessToken;
            this.requestBody = requestBody;
            return responseBody;
        }
    }
}
