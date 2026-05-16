package com.aquilaoffice.adapter.out.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.out.SlackOAuthCodeExchangeRequest;
import com.aquilaoffice.application.port.out.SlackOAuthCodeExchangeResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SlackOAuthHttpAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TestSlackApiServer slackApiServer;

    @AfterEach
    void tearDown() {
        if (slackApiServer != null) {
            slackApiServer.stop();
        }
    }

    @Test
    void exchangeCodePostsFormAndReturnsTokenResult() throws Exception {
        slackApiServer = TestSlackApiServer.start(200, """
                {
                  "ok": true,
                  "access_token": "xoxb-token-secret",
                  "team": {"id": "T123", "name": "Team"},
                  "incoming_webhook": {
                    "channel": "dev-alerts",
                    "channel_id": "C456"
                  }
                }
                """);
        SlackOAuthHttpAdapter adapter = new SlackOAuthHttpAdapter(
                " client-123 ",
                " secret-456 ",
                HttpClient.newHttpClient(),
                slackApiServer.oauthAccessUri(),
                objectMapper);

        SlackOAuthCodeExchangeResult result = adapter.exchangeCode(new SlackOAuthCodeExchangeRequest(
                " code-789 ",
                " http://localhost:8080/api/v1/slack/oauth/callback "));

        assertThat(slackApiServer.method).isEqualTo("POST");
        assertThat(slackApiServer.contentType).isEqualTo("application/x-www-form-urlencoded");
        String decodedBody = URLDecoder.decode(slackApiServer.body, StandardCharsets.UTF_8);
        assertThat(decodedBody).contains("client_id=client-123");
        assertThat(decodedBody).contains("client_secret=secret-456");
        assertThat(decodedBody).contains("code=code-789");
        assertThat(decodedBody).contains("redirect_uri=http://localhost:8080/api/v1/slack/oauth/callback");
        assertThat(result.teamId()).isEqualTo("T123");
        assertThat(result.channelId()).isEqualTo("C456");
        assertThat(result.channelName()).isEqualTo("dev-alerts");
        assertThat(result.botToken()).isEqualTo("xoxb-token-secret");
    }

    @Test
    void exchangeCodeFailsWhenSlackApiRejectsCode() throws Exception {
        slackApiServer = TestSlackApiServer.start(200, "{\"ok\":false,\"error\":\"invalid_code\"}");
        SlackOAuthHttpAdapter adapter = new SlackOAuthHttpAdapter(
                "client-123",
                "secret-456",
                HttpClient.newHttpClient(),
                slackApiServer.oauthAccessUri(),
                objectMapper);

        assertThatThrownBy(() -> adapter.exchangeCode(new SlackOAuthCodeExchangeRequest(
                        "bad-code",
                        "http://localhost:8080/api/v1/slack/oauth/callback")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("slack oauth token exchange failed: invalid_code");
    }

    private static class TestSlackApiServer {
        private final HttpServer server;
        private String method;
        private String contentType;
        private String body;

        private TestSlackApiServer(HttpServer server) {
            this.server = server;
        }

        private static TestSlackApiServer start(int statusCode, String responseBody) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            TestSlackApiServer testServer = new TestSlackApiServer(server);
            server.createContext("/oauth.v2.access", exchange -> {
                testServer.method = exchange.getRequestMethod();
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

        private URI oauthAccessUri() {
            return URI.create("http://127.0.0.1:"
                    + server.getAddress().getPort()
                    + "/oauth.v2.access");
        }

        private void stop() {
            server.stop(0);
        }
    }
}
