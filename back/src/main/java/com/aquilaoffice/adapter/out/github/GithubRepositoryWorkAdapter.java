package com.aquilaoffice.adapter.out.github;

import com.aquilaoffice.application.port.out.GithubRepositoryWorkCommand;
import com.aquilaoffice.application.port.out.GithubRepositoryWorkResult;
import com.aquilaoffice.application.port.out.RunGithubRepositoryWorkPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Repository;

/**
 * Git CLI와 GitHub PR API를 조합해 agent가 수정한 workspace를 branch/PR로 게시한다.
 */
@Repository
public class GithubRepositoryWorkAdapter implements RunGithubRepositoryWorkPort {

    private static final URI GITHUB_API_REPOS_URI = URI.create("https://api.github.com/repos/");

    private final GitCommandRunner gitCommandRunner;
    private final GithubPullRequestClient pullRequestClient;

    public GithubRepositoryWorkAdapter() {
        this(
                new ProcessGitCommandRunner(),
                new HttpGithubPullRequestClient(
                        HttpClient.newHttpClient(),
                        GITHUB_API_REPOS_URI,
                        new ObjectMapper()));
    }

    GithubRepositoryWorkAdapter(
            GitCommandRunner gitCommandRunner,
            GithubPullRequestClient pullRequestClient) {
        this.gitCommandRunner = Objects.requireNonNull(gitCommandRunner, "gitCommandRunner must not be null");
        this.pullRequestClient = Objects.requireNonNull(pullRequestClient, "pullRequestClient must not be null");
    }

    @Override
    public GithubRepositoryWorkResult run(GithubRepositoryWorkCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Path workspacePath = Path.of(command.workspacePath()).toAbsolutePath().normalize();

        prepareWorkspace(command, workspacePath);
        runLocal(workspacePath, "checkout", "-B", command.workBranch());
        runLocal(workspacePath, "config", "user.name", command.commitAuthorName());
        runLocal(workspacePath, "config", "user.email", command.commitAuthorEmail());
        runLocal(workspacePath, "add", "-A");
        if (hasWorkspaceChanges(workspacePath)) {
            runLocal(workspacePath, "commit", "-m", command.commitMessage());
        }
        runRemote(command, workspacePath, "push", "origin", "HEAD:refs/heads/" + command.workBranch());

        GithubPullRequestResponse pullRequestResponse = pullRequestClient.create(new GithubPullRequestRequest(
                command.repositoryOwner(),
                command.repositoryName(),
                command.baseBranch(),
                command.workBranch(),
                command.pullRequestTitle(),
                command.pullRequestBody(),
                command.token()));
        return new GithubRepositoryWorkResult(
                command.repositoryOwner(),
                command.repositoryName(),
                command.workBranch(),
                pullRequestResponse.pullRequestUrl());
    }

    private void prepareWorkspace(GithubRepositoryWorkCommand command, Path workspacePath) {
        if (Files.isDirectory(workspacePath.resolve(".git"))) {
            runRemote(command, workspacePath, "fetch", "origin", command.baseBranch());
            runLocal(workspacePath, "checkout", command.baseBranch());
            runRemote(command, workspacePath, "pull", "--ff-only", "origin", command.baseBranch());
            return;
        }
        createParentDirectory(workspacePath);
        runRemote(
                command,
                null,
                "clone",
                "--branch",
                command.baseBranch(),
                command.publicCloneUrl(),
                workspacePath.toString());
    }

    private boolean hasWorkspaceChanges(Path workspacePath) {
        String status = runLocal(workspacePath, "status", "--porcelain");
        return !status.isBlank();
    }

    private String runLocal(Path workingDirectory, String... arguments) {
        return gitCommandRunner.run(GitCommand.local(workingDirectory, List.of(arguments)));
    }

    private String runRemote(GithubRepositoryWorkCommand command, Path workingDirectory, String... arguments) {
        return gitCommandRunner.run(GitCommand.remote(
                workingDirectory,
                List.of(arguments),
                githubAuthEnvironment(command.token())));
    }

    private static void createParentDirectory(Path workspacePath) {
        Path parent = workspacePath.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new IllegalStateException("workspace parent directory create failed", exception);
        }
    }

    private static Map<String, String> githubAuthEnvironment(String token) {
        String basicAuth = Base64.getEncoder()
                .encodeToString(("x-access-token:" + token).getBytes(StandardCharsets.UTF_8));
        return Map.of(
                "GIT_CONFIG_COUNT", "1",
                "GIT_CONFIG_KEY_0", "http.https://github.com/.extraheader",
                "GIT_CONFIG_VALUE_0", "AUTHORIZATION: basic " + basicAuth);
    }
}
