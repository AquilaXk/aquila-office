package com.aquilaoffice.adapter.out.filesystem.artifact;

import com.aquilaoffice.application.port.out.ArtifactFileData;
import com.aquilaoffice.application.port.out.ArtifactNodeData;
import com.aquilaoffice.application.port.out.ArtifactNodeType;
import com.aquilaoffice.application.port.out.ArtifactTreeData;
import com.aquilaoffice.application.port.out.LoadWorkspaceArtifactsPort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;

/**
 * 워크스페이스별 artifact root 아래의 파일만 읽는 filesystem adapter다.
 */
@Repository
public class WorkspaceArtifactFilesystemAdapter implements LoadWorkspaceArtifactsPort {

    private static final String WORKSPACE_ROOT_DIRECTORY = "workspaces";

    private final Path artifactRoot;

    public WorkspaceArtifactFilesystemAdapter() {
        this(Path.of(".aquila", "artifacts"));
    }

    WorkspaceArtifactFilesystemAdapter(Path artifactRoot) {
        this.artifactRoot = Objects.requireNonNull(artifactRoot, "artifactRoot must not be null")
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public ArtifactTreeData loadTree(Long workspaceId) {
        Path workspaceRoot = workspaceRoot(workspaceId);
        if (!Files.isDirectory(workspaceRoot, LinkOption.NOFOLLOW_LINKS)) {
            return new ArtifactTreeData(workspaceId, "/", List.of());
        }

        return new ArtifactTreeData(workspaceId, "/", listChildren(workspaceRoot, workspaceRoot));
    }

    @Override
    public ArtifactFileData loadFile(Long workspaceId, String path) {
        Path workspaceRoot = workspaceRoot(workspaceId);
        Path artifactFile = resolveInsideWorkspace(workspaceRoot, path);
        if (!Files.isRegularFile(artifactFile, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("artifact file not found");
        }

        try {
            return new ArtifactFileData(
                    workspaceId,
                    relativePath(workspaceRoot, artifactFile),
                    artifactFile.getFileName().toString(),
                    Files.probeContentType(artifactFile),
                    Files.size(artifactFile),
                    Files.readString(artifactFile, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("artifact file read failed", exception);
        }
    }

    private List<ArtifactNodeData> listChildren(Path workspaceRoot, Path directory) {
        try (var stream = Files.list(directory)) {
            return stream.filter(path -> !Files.isSymbolicLink(path))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(path -> toNode(workspaceRoot, path))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("artifact tree read failed", exception);
        }
    }

    private ArtifactNodeData toNode(Path workspaceRoot, Path path) {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            return new ArtifactNodeData(
                    path.getFileName().toString(),
                    relativePath(workspaceRoot, path),
                    ArtifactNodeType.DIRECTORY,
                    null,
                    null,
                    listChildren(workspaceRoot, path));
        }

        return new ArtifactNodeData(
                path.getFileName().toString(),
                relativePath(workspaceRoot, path),
                ArtifactNodeType.FILE,
                probeContentType(path),
                size(path),
                List.of());
    }

    private Path workspaceRoot(Long workspaceId) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId must not be null");
        }
        return artifactRoot.resolve(WORKSPACE_ROOT_DIRECTORY).resolve(String.valueOf(workspaceId)).normalize();
    }

    private Path resolveInsideWorkspace(Path workspaceRoot, String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("artifact path must not be blank");
        }
        Path relativePath = Path.of(path).normalize();
        if (relativePath.isAbsolute() || relativePath.startsWith("..")) {
            throw new IllegalArgumentException("artifact path is outside workspace root");
        }
        Path resolvedPath = workspaceRoot.resolve(relativePath).normalize();
        if (!resolvedPath.startsWith(workspaceRoot) || Files.isSymbolicLink(resolvedPath)) {
            throw new IllegalArgumentException("artifact path is outside workspace root");
        }
        return resolvedPath;
    }

    private String relativePath(Path workspaceRoot, Path path) {
        return workspaceRoot.relativize(path).toString().replace('\\', '/');
    }

    private String probeContentType(Path path) {
        try {
            return Files.probeContentType(path);
        } catch (IOException exception) {
            return null;
        }
    }

    private Long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            throw new IllegalStateException("artifact file size read failed", exception);
        }
    }
}
