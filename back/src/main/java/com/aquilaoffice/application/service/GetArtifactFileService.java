package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ArtifactFileResult;
import com.aquilaoffice.application.port.in.GetArtifactFileQuery;
import com.aquilaoffice.application.port.in.GetArtifactFileUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceArtifactsPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import java.util.Objects;

/**
 * 워크스페이스 멤버에게 workspace artifact root 아래 단일 파일만 노출한다.
 */
public class GetArtifactFileService implements GetArtifactFileUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceArtifactsPort loadWorkspaceArtifactsPort;

    public GetArtifactFileService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceArtifactsPort loadWorkspaceArtifactsPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadWorkspaceArtifactsPort =
                Objects.requireNonNull(loadWorkspaceArtifactsPort, "loadWorkspaceArtifactsPort must not be null");
    }

    @Override
    public ArtifactFileResult getArtifactFile(GetArtifactFileQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String requesterGoogleSub = normalizeGoogleSub(query.requesterGoogleSub());
        requireExistingMember(requesterGoogleSub);
        requireJoinedWorkspace(query.workspaceId(), requesterGoogleSub);

        return ArtifactResultMapper.toFileResult(loadWorkspaceArtifactsPort.loadFile(query.workspaceId(), query.path()));
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

    private String normalizeGoogleSub(String googleSub) {
        if (googleSub == null || googleSub.isBlank()) {
            throw new IllegalArgumentException("requesterGoogleSub must not be blank");
        }
        return googleSub.trim();
    }
}
