package com.aquilaoffice.adapter.out.slack;

import com.aquilaoffice.application.port.out.ExchangeSlackOAuthCodePort;
import com.aquilaoffice.application.port.out.SlackOAuthCodeExchangeRequest;
import com.aquilaoffice.application.port.out.SlackOAuthCodeExchangeResult;
import com.aquilaoffice.global.security.BoundedHttpBodySubscribers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * Slack Web API의 oauth.v2.access로 authorization code를 bot token으로 교환한다.
 */
@Repository
public class SlackOAuthHttpAdapter implements ExchangeSlackOAuthCodePort {

    private static final URI DEFAULT_OAUTH_ACCESS_URI = URI.create("https://slack.com/api/oauth.v2.access");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final String clientId;
    private final String clientSecret;
    private final HttpClient httpClient;
    private final URI oauthAccessUri;
    private final ObjectMapper objectMapper;

    @Autowired
    public SlackOAuthHttpAdapter(
            @Value("${aquila.slack.client-id:}") String clientId,
            @Value("${aquila.slack.client-secret:}") String clientSecret,
            ObjectProvider<ObjectMapper> objectMapperProvider) {
        this(
                clientId,
                clientSecret,
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(),
                DEFAULT_OAUTH_ACCESS_URI,
                objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    public SlackOAuthHttpAdapter(String clientId, String clientSecret) {
        this(
                clientId,
                clientSecret,
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(),
                DEFAULT_OAUTH_ACCESS_URI,
                new ObjectMapper());
    }

    SlackOAuthHttpAdapter(
            String clientId,
            String clientSecret,
            HttpClient httpClient,
            URI oauthAccessUri,
            ObjectMapper objectMapper) {
        this.clientId = normalizeOptional(clientId);
        this.clientSecret = normalizeOptional(clientSecret);
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.oauthAccessUri = Objects.requireNonNull(oauthAccessUri, "oauthAccessUri must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public SlackOAuthCodeExchangeResult exchangeCode(SlackOAuthCodeExchangeRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        requireConfigured(clientId, "slack client id is not configured");
        requireConfigured(clientSecret, "slack client secret is not configured");

        try {
            HttpResponse<String> response = httpClient.send(
                    toHttpRequest(request),
                    BoundedHttpBodySubscribers.ofDefaultString());
            return toResult(response);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("slack oauth token exchange failed", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("slack oauth token exchange failed", exception);
        }
    }

    private HttpRequest toHttpRequest(SlackOAuthCodeExchangeRequest request) {
        return HttpRequest.newBuilder(oauthAccessUri)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(request), StandardCharsets.UTF_8))
                .build();
    }

    private String requestBody(SlackOAuthCodeExchangeRequest request) {
        return formParameter("client_id", clientId)
                + "&" + formParameter("client_secret", clientSecret)
                + "&" + formParameter("code", request.code())
                + "&" + formParameter("redirect_uri", request.redirectUri());
    }

    private String formParameter(String name, String value) {
        return encode(name) + "=" + encode(value);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private SlackOAuthCodeExchangeResult toResult(HttpResponse<String> response) throws JsonProcessingException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("slack oauth token exchange failed: http " + response.statusCode());
        }

        JsonNode responseBody = objectMapper.readTree(response.body());
        if (!responseBody.path("ok").asBoolean(false)) {
            throw new IllegalStateException(
                    "slack oauth token exchange failed: " + responseBody.path("error").asText("unknown_error"));
        }

        return new SlackOAuthCodeExchangeResult(
                requiredText(responseBody.path("team").path("id"), "team id"),
                requiredText(responseBody.path("incoming_webhook").path("channel_id"), "channel id"),
                optionalText(responseBody.path("incoming_webhook").path("channel")),
                requiredText(responseBody.path("access_token"), "bot token"));
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = optionalText(node);
        if (value == null) {
            throw new IllegalStateException("slack oauth token exchange missing " + fieldName);
        }
        return value;
    }

    private String optionalText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        return node.asText().trim();
    }

    private void requireConfigured(String value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
