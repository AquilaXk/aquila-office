package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ListTasksQuery;
import com.aquilaoffice.application.port.in.ListTasksResult;
import com.aquilaoffice.application.port.in.ListTasksUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTasksPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import java.util.List;
import java.util.Objects;

/**
 * 워크스페이스 멤버십을 검증한 뒤 Task 목록을 조회한다.
 */
public class ListTasksService implements ListTasksUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadTasksPort loadTasksPort;

    public ListTasksService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTasksPort loadTasksPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadTasksPort = Objects.requireNonNull(loadTasksPort, "loadTasksPort must not be null");
    }

    @Override
    public List<ListTasksResult> listTasks(ListTasksQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String requesterGoogleSub = normalizeGoogleSub(query.requesterGoogleSub());
        requireExistingMember(requesterGoogleSub);
        requireJoinedWorkspace(query.workspaceId(), requesterGoogleSub);

        return loadTasksPort.findByWorkspaceId(query.workspaceId()).stream()
                .map(TaskResultMapper::toListResult)
                .toList();
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
