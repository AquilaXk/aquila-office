package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ListTaskExecutionLogsQuery;
import com.aquilaoffice.application.port.in.ListTaskExecutionLogsUseCase;
import com.aquilaoffice.application.port.in.TaskExecutionLogResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionLogsPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SavedTaskExecutionLog;
import java.util.List;
import java.util.Objects;

/**
 * Task 실행 log를 workspace 멤버에게 시간순으로 노출한다.
 */
public class ListTaskExecutionLogsService implements ListTaskExecutionLogsUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadTaskPort loadTaskPort;
    private final LoadTaskExecutionLogsPort loadTaskExecutionLogsPort;

    public ListTaskExecutionLogsService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort,
            LoadTaskExecutionLogsPort loadTaskExecutionLogsPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "loadTaskPort must not be null");
        this.loadTaskExecutionLogsPort =
                Objects.requireNonNull(loadTaskExecutionLogsPort, "loadTaskExecutionLogsPort must not be null");
    }

    @Override
    public List<TaskExecutionLogResult> listTaskExecutionLogs(ListTaskExecutionLogsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String requesterGoogleSub = normalizeGoogleSub(query.requesterGoogleSub());
        requireExistingMember(requesterGoogleSub);
        requireJoinedWorkspace(query.workspaceId(), requesterGoogleSub);
        requireExistingTask(query.workspaceId(), query.taskId());

        return loadTaskExecutionLogsPort
                .findByWorkspaceIdAndTaskIdOrderByCreatedAtAsc(query.workspaceId(), query.taskId())
                .stream()
                .map(this::toResult)
                .toList();
    }

    private TaskExecutionLogResult toResult(SavedTaskExecutionLog log) {
        return new TaskExecutionLogResult(
                log.logId(),
                log.workspaceId(),
                log.taskId(),
                log.executionId(),
                log.agentId(),
                log.level(),
                log.message(),
                log.createdAt());
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

    private void requireExistingTask(Long workspaceId, Long taskId) {
        loadTaskPort
                .findByWorkspaceIdAndTaskId(workspaceId, taskId)
                .orElseThrow(() -> new IllegalArgumentException("task not found"));
    }

    private String normalizeGoogleSub(String googleSub) {
        if (googleSub == null || googleSub.isBlank()) {
            throw new IllegalArgumentException("requesterGoogleSub must not be blank");
        }
        return googleSub.trim();
    }
}
