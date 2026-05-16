package com.aquilaoffice.adapter.out.filesystem.artifact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.out.ArtifactNodeData;
import com.aquilaoffice.application.port.out.ArtifactNodeType;
import com.aquilaoffice.application.port.out.ArtifactTreeData;
import com.aquilaoffice.application.port.out.LoadWorkspaceArtifactsPort;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceArtifactFilesystemAdapterTest {

    @TempDir
    private Path artifactRoot;

    @Test
    void loadTreeReturnsWorkspaceFilesUnderRoot() throws Exception {
        Path workspaceRoot = artifactRoot.resolve("workspaces").resolve("101");
        Files.createDirectories(workspaceRoot.resolve("logs"));
        Files.writeString(workspaceRoot.resolve("logs").resolve("run.txt"), "작업 로그");
        Files.writeString(workspaceRoot.resolve("summary.md"), "# 결과 요약");
        LoadWorkspaceArtifactsPort adapter = new WorkspaceArtifactFilesystemAdapter(artifactRoot);

        ArtifactTreeData tree = adapter.loadTree(101L);

        assertThat(tree.workspaceId()).isEqualTo(101L);
        assertThat(tree.rootPath()).isEqualTo("/");
        assertThat(tree.children()).extracting(ArtifactNodeData::name).containsExactly("logs", "summary.md");
        ArtifactNodeData logs = tree.children().getFirst();
        assertThat(logs.type()).isEqualTo(ArtifactNodeType.DIRECTORY);
        assertThat(logs.path()).isEqualTo("logs");
        assertThat(logs.children()).extracting(ArtifactNodeData::path).containsExactly("logs/run.txt");
        ArtifactNodeData summary = tree.children().get(1);
        assertThat(summary.type()).isEqualTo(ArtifactNodeType.FILE);
        assertThat(summary.sizeBytes()).isPositive();
    }

    @Test
    void loadFileReturnsTextContent() throws Exception {
        Path workspaceRoot = artifactRoot.resolve("workspaces").resolve("101");
        Files.createDirectories(workspaceRoot.resolve("reports"));
        Files.writeString(workspaceRoot.resolve("reports").resolve("result.md"), "# 완료\n테스트 작성 완료");
        LoadWorkspaceArtifactsPort adapter = new WorkspaceArtifactFilesystemAdapter(artifactRoot);

        var file = adapter.loadFile(101L, "reports/result.md");

        assertThat(file.workspaceId()).isEqualTo(101L);
        assertThat(file.path()).isEqualTo("reports/result.md");
        assertThat(file.name()).isEqualTo("result.md");
        assertThat(file.content()).contains("테스트 작성 완료");
        assertThat(file.sizeBytes()).isPositive();
    }

    @Test
    void loadFileRejectsPathTraversal() throws Exception {
        Path workspaceRoot = artifactRoot.resolve("workspaces").resolve("101");
        Files.createDirectories(workspaceRoot);
        Files.writeString(artifactRoot.resolve("secret.txt"), "secret");
        LoadWorkspaceArtifactsPort adapter = new WorkspaceArtifactFilesystemAdapter(artifactRoot);

        assertThatThrownBy(() -> adapter.loadFile(101L, "../secret.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("artifact path is outside workspace root");
    }
}
