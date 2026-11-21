package com.aquilaoffice.adapter.out.github;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.out.GithubRepositoryWorkCommand;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GithubRepositoryWorkAdapterTest {

    @TempDir
    private Path tempDir;

    @Test
    void clonesWorkspaceCommitsPushesBranchAndCreatesPullRequest() {
        RecordingGitCommandRunner gitRunner = new RecordingGitCommandRunner(" M README.md\n");
        RecordingGithubPullRequestClient pullRequestClient =
                new RecordingGithubPullRequestClient("https://github.com/AquilaXk/aquila-office/pull/256");
        GithubRepositoryWorkAdapter adapter = new GithubRepositoryWorkAdapter(gitRunner, pullRequestClient);

        var result = adapter.run(command(tempDir.resolve("repo")));

        assertThat(result.repositoryOwner()).isEqualTo("AquilaXk");
        assertThat(result.repositoryName()).isEqualTo("aquila-office");
        assertThat(result.branchName()).isEqualTo("feat/github-work");
        assertThat(result.pullRequestUrl()).isEqualTo("https://github.com/AquilaXk/aquila-office/pull/256");
        assertThat(gitRunner.commands).hasSize(8);
        assertThat(gitRunner.commands.get(0).arguments())
                .contains("clone", "--branch", "main", "https://github.com/AquilaXk/aquila-office.git");
        assertThat(gitRunner.commands.get(1).arguments()).containsExactly("checkout", "-B", "feat/github-work");
        assertThat(gitRunner.commands.get(6).arguments()).containsExactly("commit", "-m", "feat(github): wire repository work");
        assertThat(gitRunner.commands.get(7).arguments())
                .containsExactly("push", "origin", "HEAD:refs/heads/feat/github-work");
        assertThat(gitRunner.flatArguments()).noneMatch(argument -> argument.contains("ghp_1234567890abcdef"));
        assertThat(pullRequestClient.request.owner()).isEqualTo("AquilaXk");
        assertThat(pullRequestClient.request.repository()).isEqualTo("aquila-office");
        assertThat(pullRequestClient.request.baseBranch()).isEqualTo("main");
        assertThat(pullRequestClient.request.headBranch()).isEqualTo("feat/github-work");
        assertThat(pullRequestClient.request.title()).isEqualTo("[Feat] GitHub 작업 연동 구현");
        assertThat(pullRequestClient.request.body()).isEqualTo("close #255");
        assertThat(pullRequestClient.request.token()).isEqualTo("ghp_1234567890abcdef");
    }

    @Test
    void fetchesExistingWorkspaceBeforeBranchWork() throws Exception {
        Path workspace = tempDir.resolve("repo");
        Files.createDirectories(workspace.resolve(".git"));
        RecordingGitCommandRunner gitRunner = new RecordingGitCommandRunner(" M README.md\n");
        GithubRepositoryWorkAdapter adapter = new GithubRepositoryWorkAdapter(
                gitRunner,
                new RecordingGithubPullRequestClient("https://github.com/AquilaXk/aquila-office/pull/256"));

        adapter.run(command(workspace));

        assertThat(gitRunner.commands.get(0).arguments()).containsExactly("fetch", "origin", "main");
        assertThat(gitRunner.commands.get(1).arguments()).containsExactly("checkout", "main");
        assertThat(gitRunner.commands.get(2).arguments()).containsExactly("pull", "--ff-only", "origin", "main");
        assertThat(gitRunner.commands.get(3).arguments()).containsExactly("checkout", "-B", "feat/github-work");
    }

    @Test
    void skipsCommitWhenWorkspaceHasNoChanges() {
        RecordingGitCommandRunner gitRunner = new RecordingGitCommandRunner("");
        GithubRepositoryWorkAdapter adapter = new GithubRepositoryWorkAdapter(
                gitRunner,
                new RecordingGithubPullRequestClient("https://github.com/AquilaXk/aquila-office/pull/256"));

        adapter.run(command(tempDir.resolve("repo")));

        assertThat(gitRunner.flatArguments()).doesNotContain("commit");
        assertThat(gitRunner.commands.getLast().arguments())
                .containsExactly("push", "origin", "HEAD:refs/heads/feat/github-work");
    }

    private static GithubRepositoryWorkCommand command(Path workspacePath) {
        return new GithubRepositoryWorkCommand(
                "https://github.com/AquilaXk/aquila-office.git",
                "main",
                "feat/github-work",
                workspacePath.toString(),
                "feat(github): wire repository work",
                "Aquila Bot",
                "bot@aquila.local",
                "[Feat] GitHub 작업 연동 구현",
                "close #255",
                "ghp_1234567890abcdef");
    }

    private static final class RecordingGitCommandRunner implements GitCommandRunner {
        private final List<GitCommand> commands = new ArrayList<>();
        private final String statusOutput;

        private RecordingGitCommandRunner(String statusOutput) {
            this.statusOutput = statusOutput;
        }

        @Override
        public String run(GitCommand command) {
            commands.add(command);
            return command.arguments().contains("status") ? statusOutput : "";
        }

        private List<String> flatArguments() {
            return commands.stream()
                    .flatMap(command -> command.arguments().stream())
                    .toList();
        }
    }

    private static final class RecordingGithubPullRequestClient implements GithubPullRequestClient {
        private final String pullRequestUrl;
        private GithubPullRequestRequest request;

        private RecordingGithubPullRequestClient(String pullRequestUrl) {
            this.pullRequestUrl = pullRequestUrl;
        }

        @Override
        public GithubPullRequestResponse create(GithubPullRequestRequest request) {
            this.request = request;
            return new GithubPullRequestResponse(pullRequestUrl);
        }
    }
}
