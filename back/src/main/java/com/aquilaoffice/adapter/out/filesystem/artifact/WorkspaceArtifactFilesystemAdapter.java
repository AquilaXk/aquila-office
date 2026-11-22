package com.aquilaoffice.adapter.out.filesystem.artifact;

import com.aquilaoffice.application.port.out.ArtifactFileData;
import com.aquilaoffice.application.port.out.ArtifactNodeData;
import com.aquilaoffice.application.port.out.ArtifactNodeType;
import com.aquilaoffice.application.port.out.ArtifactTreeData;
import com.aquilaoffice.application.port.out.LoadOrchestrationPlanArtifactPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceArtifactsPort;
import com.aquilaoffice.application.port.out.OrchestrationPlanArtifactData;
import com.aquilaoffice.application.port.out.OrchestrationPlanFileData;
import com.aquilaoffice.application.port.out.OrchestrationPlanStepData;
import com.aquilaoffice.application.port.out.SaveWorkspaceArtifactPort;
import com.aquilaoffice.application.port.out.WorkspaceArtifactFileCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 워크스페이스별 artifact root 아래의 파일만 읽는 filesystem adapter다.
 */
@Repository
public class WorkspaceArtifactFilesystemAdapter
        implements LoadWorkspaceArtifactsPort, LoadOrchestrationPlanArtifactPort, SaveWorkspaceArtifactPort {

    static final int MAX_TREE_DEPTH = 16;
    static final int MAX_TREE_ENTRIES = 2000;
    static final long MAX_ARTIFACT_FILE_BYTES = 2L * 1024L * 1024L;

    private static final String WORKSPACE_ROOT_DIRECTORY = "workspaces";
    private static final String PLAN_DIRECTORY = "plans";
    private static final String PLAN_DESCRIPTOR_FILE = "plan.json";

    private final Path artifactRoot;
    private final ObjectMapper objectMapper;

    @Autowired
    public WorkspaceArtifactFilesystemAdapter(ObjectProvider<ObjectMapper> objectMapperProvider) {
        this(Path.of(".aquila", "artifacts"), objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    public WorkspaceArtifactFilesystemAdapter() {
        this(Path.of(".aquila", "artifacts"), new ObjectMapper());
    }

    WorkspaceArtifactFilesystemAdapter(Path artifactRoot) {
        this(artifactRoot, new ObjectMapper());
    }

    WorkspaceArtifactFilesystemAdapter(Path artifactRoot, ObjectMapper objectMapper) {
        this.artifactRoot = Objects.requireNonNull(artifactRoot, "artifactRoot must not be null")
                .toAbsolutePath()
                .normalize();
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public ArtifactTreeData loadTree(Long workspaceId) {
        Path workspaceRoot = workspaceRoot(workspaceId);
        if (!Files.isDirectory(workspaceRoot, LinkOption.NOFOLLOW_LINKS)) {
            return new ArtifactTreeData(workspaceId, "/", List.of());
        }

        return new ArtifactTreeData(workspaceId, "/", listChildren(workspaceRoot, workspaceRoot, 0, new int[] {0}));
    }

    @Override
    public ArtifactFileData loadFile(Long workspaceId, String path) {
        Path workspaceRoot = workspaceRoot(workspaceId);
        Path artifactFile = resolveInsideWorkspace(workspaceRoot, path);
        if (!Files.isRegularFile(artifactFile, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("artifact file not found");
        }

        try {
            long fileSize = Files.size(artifactFile);
            if (fileSize > MAX_ARTIFACT_FILE_BYTES) {
                throw new IllegalArgumentException("artifact file exceeds maximum size");
            }
            byte[] contentBytes = readBoundedFileBytes(artifactFile);
            return new ArtifactFileData(
                    workspaceId,
                    relativePath(workspaceRoot, artifactFile),
                    artifactFile.getFileName().toString(),
                    Files.probeContentType(artifactFile),
                    (long) contentBytes.length,
                    new String(contentBytes, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("artifact file read failed", exception);
        }
    }

    @Override
    public ArtifactFileData saveFile(WorkspaceArtifactFileCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Path workspaceRoot = workspaceRoot(command.workspaceId());
        Path artifactFile = resolveInsideWorkspace(workspaceRoot, command.path());
        try {
            ensureWritableParent(workspaceRoot, artifactFile);
            Files.writeString(artifactFile, command.content(), StandardCharsets.UTF_8);
            return loadFile(command.workspaceId(), relativePath(workspaceRoot, artifactFile));
        } catch (IOException exception) {
            throw new IllegalStateException("artifact file write failed", exception);
        }
    }

    @Override
    public OrchestrationPlanArtifactData loadOrchestrationPlan(Long workspaceId, Long planId) {
        Path workspaceRoot = workspaceRoot(workspaceId);
        Path descriptorPath = planDescriptorPath(workspaceRoot, planId);
        if (!Files.isRegularFile(descriptorPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("orchestration plan artifact not found");
        }

        try {
            if (Files.size(descriptorPath) > MAX_ARTIFACT_FILE_BYTES) {
                throw new IllegalArgumentException("artifact file exceeds maximum size");
            }
            OrchestrationPlanDescriptor descriptor =
                    objectMapper.readValue(descriptorPath.toFile(), OrchestrationPlanDescriptor.class);
            return new OrchestrationPlanArtifactData(
                    workspaceId,
                    planId,
                    safeSteps(descriptor.steps()).stream()
                            .sorted(Comparator.comparingInt(WorkspaceArtifactFilesystemAdapter::sequenceNo))
                            .map(step -> toPlanStepData(workspaceRoot, step))
                            .toList());
        } catch (IOException exception) {
            throw new IllegalStateException("orchestration plan descriptor read failed", exception);
        }
    }

    private byte[] readBoundedFileBytes(Path artifactFile) throws IOException {
        try (InputStream inputStream = Files.newInputStream(artifactFile)) {
            int maxBytes = (int) MAX_ARTIFACT_FILE_BYTES;
            byte[] bytes = inputStream.readNBytes(maxBytes + 1);
            if (bytes.length > maxBytes) {
                throw new IllegalArgumentException("artifact file exceeds maximum size");
            }
            return bytes;
        }
    }

    private List<ArtifactNodeData> listChildren(
            Path workspaceRoot,
            Path directory,
            int depth,
            int[] visitedEntries) {
        if (depth > MAX_TREE_DEPTH) {
            throw new IllegalArgumentException("artifact tree exceeds maximum depth");
        }
        List<Path> children = new ArrayList<>();
        try (var stream = Files.list(directory)) {
            Iterator<Path> iterator = stream.iterator();
            while (iterator.hasNext()) {
                Path child = iterator.next();
                if (Files.isSymbolicLink(child)) {
                    continue;
                }
                visitedEntries[0]++;
                if (visitedEntries[0] > MAX_TREE_ENTRIES) {
                    throw new IllegalArgumentException("artifact tree exceeds maximum entries");
                }
                children.add(child);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("artifact tree read failed", exception);
        }
        children.sort(Comparator.comparing(path -> path.getFileName().toString()));
        List<ArtifactNodeData> nodes = new ArrayList<>(children.size());
        for (Path child : children) {
            nodes.add(toNode(workspaceRoot, child, depth, visitedEntries));
        }
        return List.copyOf(nodes);
    }

    private ArtifactNodeData toNode(
            Path workspaceRoot,
            Path path,
            int parentDepth,
            int[] visitedEntries) {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            return new ArtifactNodeData(
                    path.getFileName().toString(),
                    relativePath(workspaceRoot, path),
                    ArtifactNodeType.DIRECTORY,
                    null,
                    null,
                    listChildren(workspaceRoot, path, parentDepth + 1, visitedEntries));
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

    private Path planDescriptorPath(Path workspaceRoot, Long planId) {
        if (planId == null) {
            throw new IllegalArgumentException("planId must not be null");
        }
        if (planId <= 0) {
            throw new IllegalArgumentException("planId must be positive");
        }
        Path descriptorPath = workspaceRoot.resolve(PLAN_DIRECTORY)
                .resolve(String.valueOf(planId))
                .resolve(PLAN_DESCRIPTOR_FILE)
                .normalize();
        if (!descriptorPath.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("artifact path is outside workspace root");
        }
        rejectSymlinkAncestors(workspaceRoot, descriptorPath);
        return descriptorPath;
    }

    private OrchestrationPlanStepData toPlanStepData(Path workspaceRoot, OrchestrationPlanStepDescriptor step) {
        return new OrchestrationPlanStepData(
                step.stepId(),
                step.stepKey(),
                sequenceNo(step),
                step.title(),
                safeFiles(step.files()).stream()
                        .map(file -> toPlanFileData(workspaceRoot, file))
                        .toList());
    }

    private OrchestrationPlanFileData toPlanFileData(Path workspaceRoot, OrchestrationPlanFileDescriptor file) {
        Path artifactFile = resolveInsideWorkspace(workspaceRoot, file.path());
        boolean exists = Files.isRegularFile(artifactFile, LinkOption.NOFOLLOW_LINKS);
        return new OrchestrationPlanFileData(
                relativePath(workspaceRoot, artifactFile),
                fileName(file, artifactFile),
                contentType(file, artifactFile, exists),
                exists ? size(artifactFile) : null,
                exists);
    }

    private static List<OrchestrationPlanStepDescriptor> safeSteps(List<OrchestrationPlanStepDescriptor> steps) {
        return steps == null ? List.of() : steps;
    }

    private static List<OrchestrationPlanFileDescriptor> safeFiles(List<OrchestrationPlanFileDescriptor> files) {
        return files == null ? List.of() : files;
    }

    private static int sequenceNo(OrchestrationPlanStepDescriptor step) {
        return Optional.ofNullable(step.sequenceNo()).orElse(0);
    }

    private String fileName(OrchestrationPlanFileDescriptor file, Path artifactFile) {
        if (file.name() != null && !file.name().isBlank()) {
            return file.name().trim();
        }
        return artifactFile.getFileName().toString();
    }

    private String contentType(OrchestrationPlanFileDescriptor file, Path artifactFile, boolean exists) {
        if (file.contentType() != null && !file.contentType().isBlank()) {
            return file.contentType().trim();
        }
        return exists ? probeContentType(artifactFile) : null;
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
        // descriptor에 저장된 상대경로라도 workspace root 밖으로 해석되면 노출하지 않는다.
        if (!resolvedPath.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("artifact path is outside workspace root");
        }
        rejectSymlinkAncestors(workspaceRoot, resolvedPath);
        return resolvedPath;
    }

    private void ensureWritableParent(Path workspaceRoot, Path artifactFile) throws IOException {
        Path parent = artifactFile.getParent();
        if (parent == null || !parent.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("artifact path is outside workspace root");
        }
        rejectSymlinkAncestors(workspaceRoot, parent);
        Files.createDirectories(parent);
    }

    private void rejectSymlinkAncestors(Path workspaceRoot, Path targetPath) {
        if (!targetPath.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("artifact path is outside workspace root");
        }
        Path current = workspaceRoot;
        if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
            throw new IllegalArgumentException("artifact path is outside workspace root");
        }
        for (Path segment : workspaceRoot.relativize(targetPath)) {
            current = current.resolve(segment);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("artifact path is outside workspace root");
            }
        }
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

    private record OrchestrationPlanDescriptor(List<OrchestrationPlanStepDescriptor> steps) {}

    private record OrchestrationPlanStepDescriptor(
            Long stepId,
            String stepKey,
            Integer sequenceNo,
            String title,
            List<OrchestrationPlanFileDescriptor> files) {}

    private record OrchestrationPlanFileDescriptor(
            String path,
            String name,
            String contentType) {}
}
