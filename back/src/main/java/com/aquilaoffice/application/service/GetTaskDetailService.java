package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.GetTaskDetailQuery;
import com.aquilaoffice.application.port.in.GetTaskDetailResult;
import com.aquilaoffice.application.port.in.GetTaskDetailUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import java.util.Objects;

/**
 * 워크스페이스 멤버에게 해당 워크스페이스의 Task 상세만 노출한다.
 */
public class GetTaskDetailService implements GetTaskDetailUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadTaskPort loadTaskPort;

    public GetTaskDetailService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "loadTaskPort must not be null");
    }

    @Override
    public GetTaskDetailResult getTaskDetail(GetTaskDetailQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String requesterGoogleSub = normalizeGoogleSub(query.requesterGoogleSub());
        requireExistingMember(requesterGoogleSub);
        requireJoinedWorkspace(query.workspaceId(), requesterGoogleSub);

        return loadTaskPort
                .findByWorkspaceIdAndTaskId(query.workspaceId(), query.taskId())
                .map(TaskResultMapper::toDetailResult)
                .orElseThrow(() -> new IllegalArgumentException("task not found"));
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
