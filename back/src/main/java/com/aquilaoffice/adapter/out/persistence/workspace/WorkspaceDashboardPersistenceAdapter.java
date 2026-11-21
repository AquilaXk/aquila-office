package com.aquilaoffice.adapter.out.persistence.workspace;

import com.aquilaoffice.adapter.out.persistence.agent.WorkspaceAgentDashboardCounts;
import com.aquilaoffice.adapter.out.persistence.agent.WorkspaceAgentJpaRepository;
import com.aquilaoffice.adapter.out.persistence.execution.TaskExecutionJpaEntity;
import com.aquilaoffice.adapter.out.persistence.execution.TaskExecutionLogJpaEntity;
import com.aquilaoffice.adapter.out.persistence.execution.TaskExecutionLogJpaRepository;
import com.aquilaoffice.adapter.out.persistence.execution.TaskExecutionJpaRepository;
import com.aquilaoffice.adapter.out.persistence.task.TaskDashboardCounts;
import com.aquilaoffice.adapter.out.persistence.task.TaskJpaRepository;
import com.aquilaoffice.application.port.out.LoadWorkspaceDashboardSummaryPort;
import com.aquilaoffice.application.port.out.WorkspaceDashboardRecentLogData;
import com.aquilaoffice.application.port.out.WorkspaceDashboardRecentReportData;
import com.aquilaoffice.application.port.out.WorkspaceDashboardSummaryData;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.task.TaskStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 워크스페이스 대시보드용 집계 값을 기존 agent/task/execution 테이블에서 읽는 adapter다.
 */
@Repository
public class WorkspaceDashboardPersistenceAdapter implements LoadWorkspaceDashboardSummaryPort {

    private static final int RECENT_REPORT_LIMIT = 5;
    private static final int RECENT_LOG_LIMIT = 10;
    private static final List<TaskStatus> RUNNING_TASK_STATUSES =
            List.of(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS, TaskStatus.WAITING_USER);
    private static final List<WorkspaceAgentStatus> ERROR_AGENT_STATUSES =
            List.of(WorkspaceAgentStatus.ERROR, WorkspaceAgentStatus.SYNC_FAILED);

    private final WorkspaceJpaRepository workspaceJpaRepository;
    private final WorkspaceAgentJpaRepository workspaceAgentJpaRepository;
    private final TaskJpaRepository taskJpaRepository;
    private final TaskExecutionJpaRepository taskExecutionJpaRepository;
    private final TaskExecutionLogJpaRepository taskExecutionLogJpaRepository;
    private final Clock clock;

    @Autowired
    public WorkspaceDashboardPersistenceAdapter(
            WorkspaceJpaRepository workspaceJpaRepository,
            WorkspaceAgentJpaRepository workspaceAgentJpaRepository,
            TaskJpaRepository taskJpaRepository,
            TaskExecutionJpaRepository taskExecutionJpaRepository,
            TaskExecutionLogJpaRepository taskExecutionLogJpaRepository) {
        this(
                workspaceJpaRepository,
                workspaceAgentJpaRepository,
                taskJpaRepository,
                taskExecutionJpaRepository,
                taskExecutionLogJpaRepository,
                Clock.systemUTC());
    }

    WorkspaceDashboardPersistenceAdapter(
            WorkspaceJpaRepository workspaceJpaRepository,
            WorkspaceAgentJpaRepository workspaceAgentJpaRepository,
            TaskJpaRepository taskJpaRepository,
            TaskExecutionJpaRepository taskExecutionJpaRepository,
            TaskExecutionLogJpaRepository taskExecutionLogJpaRepository,
            Clock clock) {
        this.workspaceJpaRepository =
                Objects.requireNonNull(workspaceJpaRepository, "workspaceJpaRepository must not be null");
        this.workspaceAgentJpaRepository =
                Objects.requireNonNull(workspaceAgentJpaRepository, "workspaceAgentJpaRepository must not be null");
        this.taskJpaRepository = Objects.requireNonNull(taskJpaRepository, "taskJpaRepository must not be null");
        this.taskExecutionJpaRepository =
                Objects.requireNonNull(taskExecutionJpaRepository, "taskExecutionJpaRepository must not be null");
        this.taskExecutionLogJpaRepository =
                Objects.requireNonNull(taskExecutionLogJpaRepository, "taskExecutionLogJpaRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceDashboardSummaryData loadSummary(Long workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");

        WorkspaceAgentDashboardCounts agentCounts = workspaceAgentJpaRepository.countDashboardMetricsByWorkspaceId(
                workspaceId,
                WorkspaceAgentStatus.DISABLED,
                WorkspaceAgentStatus.READY,
                ERROR_AGENT_STATUSES);
        long agentCount = agentCounts.activeCount();
        long runningAgentCount = taskExecutionJpaRepository.countDistinctAgentIdByWorkspaceIdAndStatus(
                workspaceId,
                TaskExecutionStatus.RUNNING);
        long readyAgentCount = agentCounts.readyCount();
        long idleAgentCount = Math.max(0, readyAgentCount - runningAgentCount);
        long errorAgentCount = agentCounts.errorCount();

        TaskDashboardCounts taskCounts = taskJpaRepository.countDashboardMetricsByWorkspaceId(
                workspaceId,
                RUNNING_TASK_STATUSES,
                TaskStatus.WAITING_USER,
                TaskStatus.COMPLETED,
                TaskStatus.FAILED);
        long taskCount = taskCounts.totalCount();
        long runningTaskCount = taskCounts.runningCount();
        long waitingApprovalTaskCount = taskCounts.waitingApprovalCount();
        long completedTaskCount = taskCounts.completedCount();
        long failedTaskCount = taskCounts.failedCount();

        Integer dailyTokenLimit = workspaceJpaRepository.findActiveDailyTokenLimitById(workspaceId).orElse(null);
        LocalDateTime startOfTodayUtc = LocalDate.now(clock.withZone(ZoneOffset.UTC)).atStartOfDay();
        long todayUsedTokens = taskExecutionJpaRepository.sumTokensByWorkspaceIdSince(workspaceId, startOfTodayUtc);
        Double averageDurationMs = taskExecutionJpaRepository.findAverageDurationMsByWorkspaceId(workspaceId);
        Long avgExecutionDurationMs = averageDurationMs == null ? null : Math.round(averageDurationMs);

        return new WorkspaceDashboardSummaryData(
                agentCount,
                runningAgentCount,
                idleAgentCount,
                errorAgentCount,
                taskCount,
                runningTaskCount,
                completedTaskCount,
                failedTaskCount,
                dailyTokenLimit,
                todayUsedTokens,
                waitingApprovalTaskCount,
                avgExecutionDurationMs,
                recentReports(workspaceId),
                recentLogs(workspaceId));
    }

    private List<WorkspaceDashboardRecentReportData> recentReports(Long workspaceId) {
        return taskExecutionJpaRepository
                .findRecentByWorkspaceId(workspaceId, PageRequest.of(0, RECENT_REPORT_LIMIT))
                .stream()
                .map(this::toRecentReport)
                .toList();
    }

    private List<WorkspaceDashboardRecentLogData> recentLogs(Long workspaceId) {
        return taskExecutionLogJpaRepository
                .findRecentByWorkspaceId(workspaceId, PageRequest.of(0, RECENT_LOG_LIMIT))
                .stream()
                .map(this::toRecentLog)
                .toList();
    }

    private WorkspaceDashboardRecentReportData toRecentReport(TaskExecutionJpaEntity execution) {
        return new WorkspaceDashboardRecentReportData(
                execution.getId(),
                execution.getTask().getId(),
                execution.getAgentId(),
                execution.getStatus(),
                summaryOf(execution),
                execution.getFailureReason(),
                execution.getCreatedAt());
    }

    private WorkspaceDashboardRecentLogData toRecentLog(TaskExecutionLogJpaEntity log) {
        TaskExecutionJpaEntity execution = log.getExecution();
        return new WorkspaceDashboardRecentLogData(
                execution.getId(),
                execution.getTask().getId(),
                log.getAgentId(),
                log.getLevel(),
                log.getMessage(),
                log.getCreatedAt());
    }

    private static String summaryOf(TaskExecutionJpaEntity execution) {
        if (execution.getResultSummary() != null && !execution.getResultSummary().isBlank()) {
            return execution.getResultSummary();
        }
        if (execution.getFailureReason() != null && !execution.getFailureReason().isBlank()) {
            return execution.getFailureReason();
        }
        return execution.getStatus().name();
    }
}
