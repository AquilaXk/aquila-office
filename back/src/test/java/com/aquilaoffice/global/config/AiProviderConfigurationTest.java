package com.aquilaoffice.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AiProviderConfigurationTest {

    private static final List<String> FORBIDDEN_PROVIDER_TOKENS = List.of(
            "glm",
            "generativelanguage",
            "google-generative",
            "google.genai");

    @Test
    void backendUsesVertexAiProviderOnly() throws IOException {
        Path root = repositoryRoot();
        String analyzerSource = Files.readString(root.resolve(
                "back/src/main/java/com/aquilaoffice/adapter/out/ai/VertexAiChatIntentAnalyzer.java"));
        String applicationConfig = Files.readString(root.resolve("back/src/main/resources/application.yml"));

        assertThat(analyzerSource)
                .contains("@Repository")
                .contains("implements AnalyzeChatIntentPort")
                .contains("VertexAiProperties");
        assertThat(applicationConfig)
                .contains("vertex-ai:")
                .contains("project-id: ${GOOGLE_CLOUD_PROJECT_ID:}")
                .contains("model: ${VERTEX_AI_MODEL:gemini-2.5-flash}")
                .contains("credentials-path: ${GOOGLE_APPLICATION_CREDENTIALS:}");
    }

    @Test
    void backendSourceAndDependenciesDoNotUseGlmProvider() throws IOException {
        Path root = repositoryRoot();

        List<String> violations;
        try (Stream<Path> files = Stream.concat(
                Files.walk(root.resolve("back/src/main/java")),
                Stream.of(root.resolve("back/build.gradle.kts")))) {
            violations = files
                    .filter(Files::isRegularFile)
                    .filter(path -> containsForbiddenProviderToken(path, root))
                    .map(root::relativize)
                    .map(Path::toString)
                    .toList();
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void composeBackendsExposeVertexAiRuntimeSettings() throws IOException {
        Path root = repositoryRoot();

        assertComposeExposesVertex(root.resolve("compose.dev.yml"));
        assertComposeExposesVertex(root.resolve("compose.prod.yml"));
    }

    private static void assertComposeExposesVertex(Path composeFile) throws IOException {
        String compose = Files.readString(composeFile);

        assertThat(compose)
                .contains("GOOGLE_CLOUD_PROJECT_ID: ${GOOGLE_CLOUD_PROJECT_ID:-grepp-ai-project}")
                .contains("VERTEX_AI_LOCATION: ${VERTEX_AI_LOCATION:-us-central1}")
                .contains("VERTEX_AI_MODEL: ${VERTEX_AI_MODEL:-gemini-2.5-flash}")
                .contains("GOOGLE_APPLICATION_CREDENTIALS: ${GOOGLE_APPLICATION_CREDENTIALS:-/run/secrets/vertex-key.json}")
                .contains("source: ${VERTEX_AI_CREDENTIALS_HOST_PATH:-./vertex-key.json}")
                .contains("target: /run/secrets/vertex-key.json")
                .contains("read_only: true");
    }

    private static boolean containsForbiddenProviderToken(Path path, Path root) {
        try {
            String source = Files.readString(path).toLowerCase(Locale.ROOT);
            return FORBIDDEN_PROVIDER_TOKENS.stream().anyMatch(source::contains);
        } catch (IOException exception) {
            throw new IllegalStateException("AI provider configuration scan failed: " + root.relativize(path), exception);
        }
    }

    private static Path repositoryRoot() {
        return Path.of(System.getProperty("user.dir")).getParent();
    }
}
