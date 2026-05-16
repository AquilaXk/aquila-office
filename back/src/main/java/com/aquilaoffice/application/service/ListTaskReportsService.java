package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.ListTaskReportsQuery;
import com.aquilaoffice.application.port.in.ListTaskReportsUseCase;
import com.aquilaoffice.application.port.in.TaskReportResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTaskExecutionReportsPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SavedTaskExecution;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.task.TaskStatus;
import java.util.List;
import java.util.Objects;

/**
 * 워크스페이스 멤버에게 Task 실행 기록을 report 응답 모델로 노출한다.
 */
public class ListTaskReportsService implements ListTaskReportsUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadTaskPort loadTaskPort;
    private final LoadTaskExecutionReportsPort loadTaskExecutionReportsPort;

    public ListTaskReportsService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort,
            LoadTaskExecutionReportsPort loadTaskExecutionReportsPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "loadTaskPort must not be null");
        this.loadTaskExecutionReportsPort =
                Objects.requireNonNull(loadTaskExecutionReportsPort, "loadTaskExecutionReportsPort must not be null");
    }

    @Override
    public List<TaskReportResult> listTaskReports(ListTaskReportsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String requesterGoogleSub = normalizeGoogleSub(query.requesterGoogleSub());
        requireExistingMember(requesterGoogleSub);
        requireJoinedWorkspace(query.workspaceId(), requesterGoogleSub);
        requireExistingTask(query.workspaceId(), query.taskId());

        return loadTaskExecutionReportsPort
                .findByWorkspaceIdAndTaskIdOrderByCreatedAtDesc(query.workspaceId(), query.taskId())
                .stream()
                .map(this::toReportResult)
                .toList();
    }

    private TaskReportResult toReportResult(SavedTaskExecution execution) {
        return new TaskReportResult(
                execution.executionId(),
                execution.taskId(),
                toTaskStatus(execution.status()),
                execution.resultSummary(),
                execution.resultSummary(),
                execution.failureReason(),
                List.of(),
                execution.createdAt());
    }

    private TaskStatus toTaskStatus(TaskExecutionStatus status) {
        return switch (status) {
            case QUEUED -> TaskStatus.ASSIGNED;
            case RUNNING -> TaskStatus.IN_PROGRESS;
            case SUCCEEDED -> TaskStatus.COMPLETED;
            case FAILED -> TaskStatus.FAILED;
            case CANCELED -> TaskStatus.CANCELED;
        };
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
