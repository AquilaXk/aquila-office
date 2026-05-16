package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetOrchestrationPlanArtifactQuery;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanArtifactUseCase;
import com.aquilaoffice.application.port.in.OrchestrationPlanArtifactResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadOrchestrationPlanArtifactPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import java.util.Objects;

/**
 * 워크스페이스 멤버에게 해당 워크스페이스 orchestration plan artifact만 노출한다.
 */
public class GetOrchestrationPlanArtifactService implements GetOrchestrationPlanArtifactUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadOrchestrationPlanArtifactPort loadOrchestrationPlanArtifactPort;

    public GetOrchestrationPlanArtifactService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadOrchestrationPlanArtifactPort loadOrchestrationPlanArtifactPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadOrchestrationPlanArtifactPort = Objects.requireNonNull(
                loadOrchestrationPlanArtifactPort,
                "loadOrchestrationPlanArtifactPort must not be null");
    }

    @Override
    public OrchestrationPlanArtifactResult getOrchestrationPlanArtifact(GetOrchestrationPlanArtifactQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String requesterGoogleSub = normalizeGoogleSub(query.requesterGoogleSub());
        requireExistingMember(requesterGoogleSub);
        requireJoinedWorkspace(query.workspaceId(), requesterGoogleSub);

        return ArtifactResultMapper.toOrchestrationPlanResult(
                loadOrchestrationPlanArtifactPort.loadOrchestrationPlan(query.workspaceId(), query.planId()));
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
