package com.aquilaoffice.application.port.out;

import java.net.URI;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * GitHub 저장소에 branch push와 PR 생성을 수행하기 위한 outbound command다.
 */
public record GithubRepositoryWorkCommand(
        String repositoryUrl,
        String baseBranch,
        String workBranch,
        String workspacePath,
        String commitMessage,
        String commitAuthorName,
        String commitAuthorEmail,
        String pullRequestTitle,
        String pullRequestBody,
        String token) {

    private static final Pattern REPOSITORY_SEGMENT = Pattern.compile("[A-Za-z0-9_.-]+");
    private static final Pattern GIT_BRANCH = Pattern.compile(
            "^(?!-)(?!.*\\.\\.)(?!.*//)(?!.*@\\{)(?!.*\\\\)[A-Za-z0-9._/-]+(?<!/)(?<!\\.lock)$");

    public GithubRepositoryWorkCommand {
        RepositoryParts repositoryParts = parseRepositoryUrl(repositoryUrl);
        repositoryUrl = "https://github.com/" + repositoryParts.owner() + "/" + repositoryParts.name() + ".git";
        baseBranch = requireValidBranch(baseBranch, "baseBranch");
        workBranch = requireValidBranch(workBranch, "workBranch");
        workspacePath = normalizeWorkspacePath(workspacePath);
        commitMessage = requireNotBlank(commitMessage, "commitMessage");
        commitAuthorName = requireNotBlank(commitAuthorName, "commitAuthorName");
        commitAuthorEmail = requireNotBlank(commitAuthorEmail, "commitAuthorEmail");
        pullRequestTitle = requireNotBlank(pullRequestTitle, "pullRequestTitle");
        pullRequestBody = pullRequestBody == null ? "" : pullRequestBody.trim();
        token = requireNotBlank(token, "token");
    }

    public String repositoryOwner() {
        return parseRepositoryUrl(repositoryUrl).owner();
    }

    public String repositoryName() {
        return parseRepositoryUrl(repositoryUrl).name();
    }

    public String publicCloneUrl() {
        return repositoryUrl;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String requireValidBranch(String value, String fieldName) {
        String normalized = requireNotBlank(value, fieldName);
        if (!GIT_BRANCH.matcher(normalized).matches()) {
            throw new IllegalArgumentException(fieldName + " is invalid");
        }
        return normalized;
    }

    private static String normalizeWorkspacePath(String workspacePath) {
        String normalized = requireNotBlank(workspacePath, "workspacePath");
        return Path.of(normalized).normalize().toString();
    }

    private static RepositoryParts parseRepositoryUrl(String repositoryUrl) {
        String normalized = requireNotBlank(repositoryUrl, "repositoryUrl");
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("repositoryUrl must be a GitHub HTTPS URL", exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"github.com".equalsIgnoreCase(uri.getHost())
                || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("repositoryUrl must be a GitHub HTTPS URL");
        }
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("repositoryUrl must be a GitHub HTTPS URL");
        }
        String[] segments = path.replaceFirst("^/", "").split("/");
        if (segments.length != 2) {
            throw new IllegalArgumentException("repositoryUrl must be a GitHub HTTPS URL");
        }
        String owner = segments[0];
        String name = segments[1].replaceFirst("\\.git$", "");
        if (!REPOSITORY_SEGMENT.matcher(owner).matches() || !REPOSITORY_SEGMENT.matcher(name).matches()) {
            throw new IllegalArgumentException("repositoryUrl must be a GitHub HTTPS URL");
        }
        return new RepositoryParts(owner, name);
    }

    private record RepositoryParts(String owner, String name) {}
}
