package com.aquilaoffice.adapter.out.filesystem.artifact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.out.ArtifactNodeData;
import com.aquilaoffice.application.port.out.ArtifactNodeType;
import com.aquilaoffice.application.port.out.ArtifactTreeData;
import com.aquilaoffice.application.port.out.LoadOrchestrationPlanArtifactPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceArtifactsPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceArtifactPort;
import com.aquilaoffice.application.port.out.WorkspaceArtifactFileCommand;
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
    void saveFileWritesContentUnderWorkspaceRoot() {
        SaveWorkspaceArtifactPort adapter = new WorkspaceArtifactFilesystemAdapter(artifactRoot);

        var saved = adapter.saveFile(new WorkspaceArtifactFileCommand(
                101L,
                "reports/result.md",
                "# 완료\n테스트 작성 완료"));

        assertThat(saved.workspaceId()).isEqualTo(101L);
        assertThat(saved.path()).isEqualTo("reports/result.md");
        assertThat(saved.name()).isEqualTo("result.md");
        assertThat(saved.content()).contains("테스트 작성 완료");
        assertThat(saved.sizeBytes()).isPositive();
    }

    @Test
    void saveFileRejectsPathTraversal() {
        SaveWorkspaceArtifactPort adapter = new WorkspaceArtifactFilesystemAdapter(artifactRoot);

        assertThatThrownBy(() -> adapter.saveFile(new WorkspaceArtifactFileCommand(
                        101L,
                        "../secret.txt",
                        "secret")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("artifact path is outside workspace root");
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

    @Test
    void loadOrchestrationPlanReadsDescriptorAndFileState() throws Exception {
        Path workspaceRoot = artifactRoot.resolve("workspaces").resolve("101");
        Path planRoot = workspaceRoot.resolve("plans").resolve("701");
        Files.createDirectories(planRoot.resolve("reports"));
        Files.writeString(planRoot.resolve("reports").resolve("requirements.md"), "# 요구사항\n확인 완료");
        Files.writeString(planRoot.resolve("plan.json"), """
                {
                  "steps": [
                    {
                      "stepId": 11,
                      "stepKey": "requirements",
                      "sequenceNo": 1,
                      "title": "요구사항 확인",
                      "files": [
                        {
                          "path": "plans/701/reports/requirements.md",
                          "name": "requirements.md",
                          "contentType": "text/markdown"
                        },
                        {
                          "path": "plans/701/reports/missing.md",
                          "name": "missing.md",
                          "contentType": "text/markdown"
                        }
                      ]
                    }
                  ]
                }
                """);
        LoadOrchestrationPlanArtifactPort adapter = new WorkspaceArtifactFilesystemAdapter(artifactRoot);

        var artifact = adapter.loadOrchestrationPlan(101L, 701L);

        assertThat(artifact.workspaceId()).isEqualTo(101L);
        assertThat(artifact.planId()).isEqualTo(701L);
        assertThat(artifact.steps()).hasSize(1);
        assertThat(artifact.steps().getFirst().stepKey()).isEqualTo("requirements");
        assertThat(artifact.steps().getFirst().files()).hasSize(2);
        assertThat(artifact.steps().getFirst().files().getFirst().exists()).isTrue();
        assertThat(artifact.steps().getFirst().files().getFirst().sizeBytes()).isPositive();
        assertThat(artifact.steps().getFirst().files().get(1).exists()).isFalse();
        assertThat(artifact.steps().getFirst().files().get(1).sizeBytes()).isNull();
    }

    @Test
    void loadOrchestrationPlanRejectsUnsafeFilePath() throws Exception {
        Path workspaceRoot = artifactRoot.resolve("workspaces").resolve("101");
        Path planRoot = workspaceRoot.resolve("plans").resolve("701");
        Files.createDirectories(planRoot);
        Files.writeString(artifactRoot.resolve("secret.txt"), "secret");
        Files.writeString(planRoot.resolve("plan.json"), """
                {
                  "steps": [
                    {
                      "stepId": 11,
                      "stepKey": "requirements",
                      "sequenceNo": 1,
                      "title": "요구사항 확인",
                      "files": [
                        {
                          "path": "../secret.txt",
                          "name": "secret.txt",
                          "contentType": "text/plain"
                        }
                      ]
                    }
                  ]
                }
                """);
        LoadOrchestrationPlanArtifactPort adapter = new WorkspaceArtifactFilesystemAdapter(artifactRoot);

        assertThatThrownBy(() -> adapter.loadOrchestrationPlan(101L, 701L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("artifact path is outside workspace root");
    }
}
