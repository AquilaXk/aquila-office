package com.aquilaoffice.application.port.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GithubRepositoryWorkCommandTest {

    @Test
    void normalizesGithubRepositoryWorkInputs() {
        GithubRepositoryWorkCommand command = new GithubRepositoryWorkCommand(
                " https://github.com/AquilaXk/aquila-office.git ",
                " main ",
                " feat/github-work ",
                " /tmp/aquila-office ",
                " feat(github): wire repository work ",
                " Aquila Bot ",
                " bot@aquila.local ",
                " [Feat] GitHub 작업 연동 구현 ",
                " close #255 ",
                " ghp_1234567890abcdef ");

        assertThat(command.repositoryUrl()).isEqualTo("https://github.com/AquilaXk/aquila-office.git");
        assertThat(command.repositoryOwner()).isEqualTo("AquilaXk");
        assertThat(command.repositoryName()).isEqualTo("aquila-office");
        assertThat(command.publicCloneUrl()).isEqualTo("https://github.com/AquilaXk/aquila-office.git");
        assertThat(command.baseBranch()).isEqualTo("main");
        assertThat(command.workBranch()).isEqualTo("feat/github-work");
        assertThat(command.workspacePath()).isEqualTo(Path.of("/tmp/aquila-office").toString());
        assertThat(command.commitMessage()).isEqualTo("feat(github): wire repository work");
        assertThat(command.commitAuthorName()).isEqualTo("Aquila Bot");
        assertThat(command.commitAuthorEmail()).isEqualTo("bot@aquila.local");
        assertThat(command.pullRequestTitle()).isEqualTo("[Feat] GitHub 작업 연동 구현");
        assertThat(command.pullRequestBody()).isEqualTo("close #255");
        assertThat(command.token()).isEqualTo("ghp_1234567890abcdef");
    }

    @Test
    void rejectsInvalidGithubRepositoryWorkInputs() {
        assertThatThrownBy(() -> validCommand(" ", "main", "feat/work", "/tmp/aquila-office", "token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("repositoryUrl must not be blank");
        assertThatThrownBy(() -> validCommand(
                        "https://git.example.com/AquilaXk/aquila-office.git",
                        "main",
                        "feat/work",
                        "/tmp/aquila-office",
                        "token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("repositoryUrl must be a GitHub HTTPS URL");
        assertThatThrownBy(() -> validCommand(
                        "https://github.com/AquilaXk/aquila-office.git",
                        "-main",
                        "feat/work",
                        "/tmp/aquila-office",
                        "token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("baseBranch is invalid");
        assertThatThrownBy(() -> validCommand(
                        "https://github.com/AquilaXk/aquila-office.git",
                        "main",
                        "feat/../work",
                        "/tmp/aquila-office",
                        "token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workBranch is invalid");
        assertThatThrownBy(() -> validCommand(
                        "https://github.com/AquilaXk/aquila-office.git",
                        "main",
                        "feat/work",
                        " ",
                        "token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspacePath must not be blank");
        assertThatThrownBy(() -> validCommand(
                        "https://github.com/AquilaXk/aquila-office.git",
                        "main",
                        "feat/work",
                        "/tmp/aquila-office",
                        " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("token must not be blank");
    }

    private static GithubRepositoryWorkCommand validCommand(
            String repositoryUrl,
            String baseBranch,
            String workBranch,
            String workspacePath,
            String token) {
        return new GithubRepositoryWorkCommand(
                repositoryUrl,
                baseBranch,
                workBranch,
                workspacePath,
                "feat(github): wire repository work",
                "Aquila Bot",
                "bot@aquila.local",
                "[Feat] GitHub 작업 연동 구현",
                "close #255",
                token);
    }
}
