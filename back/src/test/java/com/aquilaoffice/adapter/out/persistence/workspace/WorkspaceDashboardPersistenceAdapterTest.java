package com.aquilaoffice.adapter.out.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.adapter.out.persistence.execution.TaskExecutionPersistenceAdapter;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.adapter.out.persistence.task.TaskPersistenceAdapter;
import com.aquilaoffice.application.port.out.LoadWorkspaceDashboardSummaryPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionLogPort;
import com.aquilaoffice.application.port.out.SaveTaskExecutionPort;
import com.aquilaoffice.application.port.out.SaveTaskPort;
import com.aquilaoffice.application.port.out.TaskExecutionLogCommand;
import com.aquilaoffice.domain.execution.TaskExecution;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.task.Task;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.domain.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TaskPersistenceAdapter.class, TaskExecutionPersistenceAdapter.class, WorkspaceDashboardPersistenceAdapter.class})
class WorkspaceDashboardPersistenceAdapterTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private SaveTaskPort saveTaskPort;

    @Autowired
    private SaveTaskExecutionPort saveTaskExecutionPort;

    @Autowired
    private SaveTaskExecutionLogPort saveTaskExecutionLogPort;

    @Autowired
    private LoadWorkspaceDashboardSummaryPort loadWorkspaceDashboardSummaryPort;

    @Test
    void loadSummaryReturnsRecentExecutionLogsNewestFirst() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        var task = saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "creator-google",
                "테스트 작성",
                null,
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L));
        var olderExecution = saveTaskExecutionPort.save(
                TaskExecution.create(workspaceEntity.getId(), task.taskId(), 501L));
        saveTaskExecutionLogPort.save(new TaskExecutionLogCommand(
                workspaceEntity.getId(),
                task.taskId(),
                olderExecution.executionId(),
                501L,
                "INFO",
                "Task execution queued"));
        var newerExecution = saveTaskExecutionPort.save(
                TaskExecution.create(workspaceEntity.getId(), task.taskId(), 501L));
        saveTaskExecutionLogPort.save(new TaskExecutionLogCommand(
                workspaceEntity.getId(),
                task.taskId(),
                newerExecution.executionId(),
                501L,
                "ERROR",
                "gateway timeout"));

        var summary = loadWorkspaceDashboardSummaryPort.loadSummary(workspaceEntity.getId());

        assertThat(summary.recentLogs()).hasSize(2);
        assertThat(summary.recentLogs()).extracting("executionId")
                .containsExactly(newerExecution.executionId(), olderExecution.executionId());
        assertThat(summary.recentLogs().getFirst().level()).isEqualTo("ERROR");
        assertThat(summary.recentLogs().getFirst().message()).isEqualTo("gateway timeout");
    }

    private WorkspaceJpaEntity saveWorkspaceEntity(String googleSub) {
        MemberJpaEntity creatorEntity = memberJpaRepository.save(MemberJpaEntity.create(
                googleSub, googleSub + "@example.com", "Creator", MemberRole.USER));
        Workspace workspace =
                Workspace.create("Team", "Description", Member.createUser(googleSub, googleSub + "@example.com", "Creator"));
        return workspaceJpaRepository.save(WorkspaceJpaEntity.from(workspace, creatorEntity));
    }
}
