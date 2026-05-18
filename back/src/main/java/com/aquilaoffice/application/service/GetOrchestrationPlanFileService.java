package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ArtifactFileResult;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanFileQuery;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanFileUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceArtifactsPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import java.nio.file.Path;
import java.util.Objects;

/**
 * 워크스페이스 멤버에게 orchestration plan 디렉터리 내부의 artifact 파일만 노출한다.
 */
public class GetOrchestrationPlanFileService implements GetOrchestrationPlanFileUseCase {

    private static final String PLAN_DIRECTORY = "plans";

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceArtifactsPort loadWorkspaceArtifactsPort;

    public GetOrchestrationPlanFileService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceArtifactsPort loadWorkspaceArtifactsPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadWorkspaceArtifactsPort =
                Objects.requireNonNull(loadWorkspaceArtifactsPort, "loadWorkspaceArtifactsPort must not be null");
    }

    @Override
    public ArtifactFileResult getOrchestrationPlanFile(GetOrchestrationPlanFileQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String requesterGoogleSub = normalizeGoogleSub(query.requesterGoogleSub());
        requireExistingMember(requesterGoogleSub);
        requireJoinedWorkspace(query.workspaceId(), requesterGoogleSub);

        String planFilePath = planFilePath(query.planId(), query.path());
        return ArtifactResultMapper.toFileResult(
                loadWorkspaceArtifactsPort.loadFile(query.workspaceId(), planFilePath));
    }

    private void requireExistingMember(String googleSub) {
        loadMemberPort
                .findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
    }

    private void requireJoinedWorkspace(Long workspaceId, String googleSub) {
        loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
    }

    private String planFilePath(Long planId, String path) {
        if (planId == null || planId <= 0) {
            throw new IllegalArgumentException("planId must be positive");
        }
        return PLAN_DIRECTORY + "/" + planId + "/" + normalizePlanRelativePath(path);
    }

    private String normalizePlanRelativePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("artifact path must not be blank");
        }
        Path relativePath = Path.of(path.trim().replace('\\', '/')).normalize();
        String normalizedPath = relativePath.toString().replace('\\', '/');
        if (relativePath.isAbsolute() || relativePath.startsWith("..") || normalizedPath.isBlank()) {
            throw new IllegalArgumentException("artifact path is outside orchestration plan root");
        }
        return normalizedPath;
    }

    private String normalizeGoogleSub(String googleSub) {
        if (googleSub == null || googleSub.isBlank()) {
            throw new IllegalArgumentException("requesterGoogleSub must not be blank");
        }
        return googleSub.trim();
    }
}
