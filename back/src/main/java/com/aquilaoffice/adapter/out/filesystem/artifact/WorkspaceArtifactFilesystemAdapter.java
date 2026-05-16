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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 워크스페이스별 artifact root 아래의 파일만 읽는 filesystem adapter다.
 */
@Repository
public class WorkspaceArtifactFilesystemAdapter implements LoadWorkspaceArtifactsPort, LoadOrchestrationPlanArtifactPort {

    private static final String WORKSPACE_ROOT_DIRECTORY = "workspaces";
    private static final String PLAN_DIRECTORY = "plans";
    private static final String PLAN_DESCRIPTOR_FILE = "plan.json";

    private final Path artifactRoot;
    private final ObjectMapper objectMapper;

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

    @Override
    public OrchestrationPlanArtifactData loadOrchestrationPlan(Long workspaceId, Long planId) {
        Path workspaceRoot = workspaceRoot(workspaceId);
        Path descriptorPath = planDescriptorPath(workspaceRoot, planId);
        if (!Files.isRegularFile(descriptorPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("orchestration plan artifact not found");
        }

        try {
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
        if (!descriptorPath.startsWith(workspaceRoot) || Files.isSymbolicLink(descriptorPath)) {
            throw new IllegalArgumentException("artifact path is outside workspace root");
        }
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
