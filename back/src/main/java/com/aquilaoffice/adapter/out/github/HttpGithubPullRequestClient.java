package com.aquilaoffice.adapter.out.github;

import com.aquilaoffice.global.security.BoundedHttpBodySubscribers;
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
import java.util.Map;
import java.util.regex.Pattern;

final class HttpGithubPullRequestClient implements GithubPullRequestClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final Pattern SAFE_REPO_SEGMENT = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    private final HttpClient httpClient;
    private final URI apiReposUri;
    private final ObjectMapper objectMapper;

    HttpGithubPullRequestClient(HttpClient httpClient, URI apiReposUri, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.apiReposUri = apiReposUri;
        this.objectMapper = objectMapper;
    }

    @Override
    public GithubPullRequestResponse create(GithubPullRequestRequest request) {
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "title", request.title(),
                    "body", request.body(),
                    "head", request.headBranch(),
                    "base", request.baseBranch()));
            HttpRequest httpRequest = HttpRequest.newBuilder(pullRequestUri(request))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + request.token())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response =
                    httpClient.send(httpRequest, BoundedHttpBodySubscribers.ofDefaultString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("github pull request create failed: " + response.statusCode());
            }
            JsonNode responseBody = objectMapper.readTree(response.body());
            String pullRequestUrl = responseBody.path("html_url").asText();
            if (pullRequestUrl == null || pullRequestUrl.isBlank()) {
                throw new IllegalStateException("github pull request response is missing html_url");
            }
            return new GithubPullRequestResponse(pullRequestUrl);
        } catch (IOException exception) {
            throw new IllegalStateException("github pull request create failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("github pull request create interrupted", exception);
        }
    }

    private URI pullRequestUri(GithubPullRequestRequest request) {
        String owner = encodeSegment(request.owner(), "owner");
        String repository = encodeSegment(request.repository(), "repository");
        String base = apiReposUri.toString();
        if (!base.endsWith("/")) {
            base += "/";
        }
        return URI.create(base + owner + "/" + repository + "/pulls");
    }

    private static String encodeSegment(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        String trimmed = raw.trim();
        if (".".equals(trimmed) || "..".equals(trimmed) || !SAFE_REPO_SEGMENT.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(fieldName + " contains invalid path characters");
        }
        return URLEncoder.encode(trimmed, StandardCharsets.UTF_8);
    }
}
