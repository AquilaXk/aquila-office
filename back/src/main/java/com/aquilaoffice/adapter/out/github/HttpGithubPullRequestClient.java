package com.aquilaoffice.adapter.out.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

final class HttpGithubPullRequestClient implements GithubPullRequestClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

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
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
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
        String base = apiReposUri.toString();
        if (!base.endsWith("/")) {
            base += "/";
        }
        return URI.create(base + request.owner() + "/" + request.repository() + "/pulls");
    }
}
