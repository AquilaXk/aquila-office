package com.aquilaoffice.adapter.in.web.slack;

import com.aquilaoffice.domain.slack.SlackOAuthState;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Slack 앱 설치 시작 URL을 생성하는 작은 boundary다.
 */
@Component
public class SlackInstallUrlBuilder {

    private static final URI DEFAULT_AUTHORIZE_URI = URI.create("https://slack.com/oauth/v2/authorize");
    private static final String DEFAULT_SCOPE = "app_mentions:read,channels:read,chat:write,incoming-webhook";

    private final String clientId;
    private final String redirectUri;
    private final String scopes;
    private final URI authorizeUri;

    @Autowired
    public SlackInstallUrlBuilder(
            @Value("${aquila.slack.client-id:}") String clientId,
            @Value("${aquila.slack.redirect-uri:http://localhost:8080/api/v1/slack/oauth/callback}")
                    String redirectUri,
            @Value("${aquila.slack.scopes:" + DEFAULT_SCOPE + "}") String scopes) {
        this(clientId, redirectUri, DEFAULT_AUTHORIZE_URI, scopes);
    }

    SlackInstallUrlBuilder(String clientId, String redirectUri, URI authorizeUri) {
        this(clientId, redirectUri, authorizeUri, DEFAULT_SCOPE);
    }

    SlackInstallUrlBuilder(String clientId, String redirectUri, URI authorizeUri, String scopes) {
        this.clientId = normalizeOptional(clientId);
        this.redirectUri = requireNotBlank(redirectUri, "redirectUri");
        this.scopes = requireNotBlank(scopes, "scopes");
        this.authorizeUri = Objects.requireNonNull(authorizeUri, "authorizeUri must not be null");
    }

    public String build(Long workspaceId, String requesterGoogleSub) {
        if (clientId == null) {
            throw new IllegalStateException("slack client id is not configured");
        }
        String state = SlackOAuthState.serialize(
                requirePositive(workspaceId, "workspaceId"),
                requireNotBlank(requesterGoogleSub, "requesterGoogleSub"));

        return authorizeUri + "?"
                + queryParameter("client_id", clientId)
                + "&" + queryParameter("scope", scopes)
                + "&" + queryParameter("redirect_uri", redirectUri)
                + "&" + queryParameter("state", state);
    }

    private String queryParameter(String name, String value) {
        return encode(name) + "=" + encode(value);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
