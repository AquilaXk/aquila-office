package com.aquilaoffice.global.config.properties;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Vertex AI Gemini 호출에 필요한 project/location/model/credentials 설정이다.
 */
@ConfigurationProperties(prefix = "aquila.vertex-ai")
public record VertexAiProperties(
        String projectId,
        String location,
        String model,
        String credentialsPath) {

    private static final String DEFAULT_LOCATION = "us-central1";
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";

    public VertexAiProperties {
        projectId = normalizeNullable(projectId);
        location = defaultIfBlank(location, DEFAULT_LOCATION);
        model = defaultIfBlank(model, DEFAULT_MODEL);
        credentialsPath = normalizeNullable(credentialsPath);
    }

    public boolean isConfigured() {
        return projectId != null && credentialsPath != null;
    }

    public URI generateContentEndpoint() {
        return URI.create("https://" + location + "-aiplatform.googleapis.com/v1/projects/"
                + projectId
                + "/locations/" + location
                + "/publishers/google/models/" + model
                + ":generateContent");
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        String normalizedValue = normalizeNullable(value);
        return normalizedValue == null ? defaultValue : normalizedValue;
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
