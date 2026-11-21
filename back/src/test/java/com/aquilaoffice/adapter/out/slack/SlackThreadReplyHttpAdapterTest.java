package com.aquilaoffice.adapter.out.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.out.LoadSlackChannelMappingPort;
import com.aquilaoffice.application.port.out.SlackReplyRequest;
import com.aquilaoffice.domain.slack.SlackChannelMapping;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SlackThreadReplyHttpAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TestSlackApiServer slackApiServer;

    @AfterEach
    void tearDown() {
        if (slackApiServer != null) {
            slackApiServer.stop();
        }
    }

    @Test
    void sendReplyPostsThreadMessageWithMappedBotToken() throws Exception {
        slackApiServer = TestSlackApiServer.start(200, "{\"ok\":true}");
        FakeLoadSlackChannelMappingPort loadMappingPort = new FakeLoadSlackChannelMappingPort(Optional.of(
                SlackChannelMapping.restore(
                        801L,
                        101L,
                        "T123",
                        "C456",
                        "dev-alerts",
                        "xoxb-token-secret",
                        "google-1",
                        true)));
        SlackThreadReplyHttpAdapter adapter = new SlackThreadReplyHttpAdapter(
                loadMappingPort,
                HttpClient.newHttpClient(),
                slackApiServer.postMessageUri(),
                objectMapper);

        adapter.sendReply(SlackReplyRequest.fromSourceRef(
                "T123:C456:1715841000.000100",
                "구현 완료",
                "task-execution-701"));

        assertThat(loadMappingPort.teamId).isEqualTo("T123");
        assertThat(loadMappingPort.channelId).isEqualTo("C456");
        assertThat(slackApiServer.method).isEqualTo("POST");
        assertThat(slackApiServer.authorization).isEqualTo("Bearer xoxb-token-secret");
        assertThat(slackApiServer.contentType).isEqualTo("application/json");
        JsonNode requestBody = objectMapper.readTree(slackApiServer.body);
        assertThat(requestBody.path("channel").asText()).isEqualTo("C456");
        assertThat(requestBody.path("text").asText()).isEqualTo("구현 완료");
        assertThat(requestBody.path("thread_ts").asText()).isEqualTo("1715841000.000100");
    }

    @Test
    void sendReplyFailsWhenActiveChannelMappingIsMissing() {
        SlackThreadReplyHttpAdapter adapter = new SlackThreadReplyHttpAdapter(
                new FakeLoadSlackChannelMappingPort(Optional.empty()),
                HttpClient.newHttpClient(),
                URI.create("http://127.0.0.1:1/chat.postMessage"),
                objectMapper);

        assertThatThrownBy(() -> adapter.sendReply(SlackReplyRequest.fromSourceRef(
                        "T123:C456:1715841000.000100",
                        "구현 완료",
                        "task-execution-701")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("slack channel mapping not found");
    }

    @Test
    void sendReplyFailsWhenSlackApiRejectsRequest() throws Exception {
        slackApiServer = TestSlackApiServer.start(200, "{\"ok\":false,\"error\":\"channel_not_found\"}");
        SlackThreadReplyHttpAdapter adapter = new SlackThreadReplyHttpAdapter(
                new FakeLoadSlackChannelMappingPort(Optional.of(SlackChannelMapping.restore(
                        801L,
                        101L,
                        "T123",
                        "C456",
                        "dev-alerts",
                        "xoxb-token-secret",
                        "google-1",
                        true))),
                HttpClient.newHttpClient(),
                slackApiServer.postMessageUri(),
                objectMapper);

        assertThatThrownBy(() -> adapter.sendReply(SlackReplyRequest.fromSourceRef(
                        "T123:C456:1715841000.000100",
                        "구현 완료",
                        "task-execution-701")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("slack reply send failed: channel_not_found");
    }

    private static class FakeLoadSlackChannelMappingPort implements LoadSlackChannelMappingPort {
        private final Optional<SlackChannelMapping> mapping;
        private String teamId;
        private String channelId;

        private FakeLoadSlackChannelMappingPort(Optional<SlackChannelMapping> mapping) {
            this.mapping = mapping;
        }

        @Override
        public Optional<SlackChannelMapping> findActiveByIdAndWorkspaceId(Long mappingId, Long workspaceId) {
            throw new UnsupportedOperationException("not used in slack thread reply adapter test");
        }

        @Override
        public Optional<SlackChannelMapping> findActiveByTeamIdAndChannelId(String teamId, String channelId) {
            this.teamId = teamId;
            this.channelId = channelId;
            return mapping;
        }

        @Override
        public List<SlackChannelMapping> findActiveByWorkspaceId(Long workspaceId) {
            throw new UnsupportedOperationException("not used in slack thread reply adapter test");
        }
    }

    private static class TestSlackApiServer {
        private final HttpServer server;
        private String method;
        private String authorization;
        private String contentType;
        private String body;

        private TestSlackApiServer(HttpServer server) {
            this.server = server;
        }

        private static TestSlackApiServer start(int statusCode, String responseBody) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            TestSlackApiServer testServer = new TestSlackApiServer(server);
            server.createContext("/chat.postMessage", exchange -> {
                testServer.method = exchange.getRequestMethod();
                testServer.authorization = exchange.getRequestHeaders().getFirst("Authorization");
                testServer.contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                testServer.body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(statusCode, responseBytes.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(responseBytes);
                }
            });
            server.start();
            return testServer;
        }

        private URI postMessageUri() {
            return URI.create("http://127.0.0.1:"
                    + server.getAddress().getPort()
                    + "/chat.postMessage");
        }

        private void stop() {
            server.stop(0);
        }
    }
}
