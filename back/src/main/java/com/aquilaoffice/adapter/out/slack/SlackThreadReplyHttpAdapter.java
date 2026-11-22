package com.aquilaoffice.adapter.out.slack;

import com.aquilaoffice.application.port.out.LoadSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.SendSlackReplyPort;
import com.aquilaoffice.application.port.out.SlackReplyRequest;
import com.aquilaoffice.domain.slack.SlackChannelMapping;
import com.aquilaoffice.global.security.BoundedHttpBodySubscribers;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Slack Web API의 chat.postMessage로 작업 결과를 원본 thread에 전송한다.
 */
@Repository
public class SlackThreadReplyHttpAdapter implements SendSlackReplyPort {

    private static final URI DEFAULT_POST_MESSAGE_URI = URI.create("https://slack.com/api/chat.postMessage");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final LoadSlackChannelMappingPort loadSlackChannelMappingPort;
    private final HttpClient httpClient;
    private final URI postMessageUri;
    private final ObjectMapper objectMapper;

    @Autowired
    public SlackThreadReplyHttpAdapter(
            LoadSlackChannelMappingPort loadSlackChannelMappingPort,
            ObjectProvider<ObjectMapper> objectMapperProvider) {
        this(
                loadSlackChannelMappingPort,
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(),
                DEFAULT_POST_MESSAGE_URI,
                objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    public SlackThreadReplyHttpAdapter(LoadSlackChannelMappingPort loadSlackChannelMappingPort) {
        this(
                loadSlackChannelMappingPort,
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(),
                DEFAULT_POST_MESSAGE_URI,
                new ObjectMapper());
    }

    SlackThreadReplyHttpAdapter(
            LoadSlackChannelMappingPort loadSlackChannelMappingPort,
            HttpClient httpClient,
            URI postMessageUri,
            ObjectMapper objectMapper) {
        this.loadSlackChannelMappingPort =
                Objects.requireNonNull(loadSlackChannelMappingPort, "loadSlackChannelMappingPort must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.postMessageUri = Objects.requireNonNull(postMessageUri, "postMessageUri must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public void sendReply(SlackReplyRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        SlackChannelMapping mapping = loadSlackChannelMappingPort
                .findActiveByTeamIdAndChannelId(request.teamId(), request.channelId())
                .orElseThrow(() -> new IllegalArgumentException("slack channel mapping not found"));

        try {
            HttpResponse<String> response = httpClient.send(
                    toHttpRequest(mapping, request),
                    BoundedHttpBodySubscribers.ofDefaultString());
            assertSuccessful(response);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("slack reply send failed", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("slack reply send failed", exception);
        }
    }

    private HttpRequest toHttpRequest(SlackChannelMapping mapping, SlackReplyRequest request)
            throws JsonProcessingException {
        return HttpRequest.newBuilder(postMessageUri)
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + mapping.getBotToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(request), StandardCharsets.UTF_8))
                .build();
    }

    private String requestBody(SlackReplyRequest request) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new SlackPostMessageRequest(
                request.channelId(),
                request.text(),
                request.threadTs()));
    }

    private void assertSuccessful(HttpResponse<String> response) throws JsonProcessingException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("slack reply send failed: http " + response.statusCode());
        }

        JsonNode responseBody = objectMapper.readTree(response.body());
        if (!responseBody.path("ok").asBoolean(false)) {
            throw new IllegalStateException(
                    "slack reply send failed: " + responseBody.path("error").asText("unknown_error"));
        }
    }

    private record SlackPostMessageRequest(
            String channel,
            String text,
            @JsonProperty("thread_ts") String threadTs) {}
}
