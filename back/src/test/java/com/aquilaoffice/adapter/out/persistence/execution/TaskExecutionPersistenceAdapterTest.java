package com.aquilaoffice.adapter.out.persistence.execution;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.adapter.out.persistence.task.TaskJpaRepository;
import com.aquilaoffice.adapter.out.persistence.task.TaskPersistenceAdapter;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.SaveTaskExecutionPort;
import com.aquilaoffice.application.port.out.SaveTaskPort;
import com.aquilaoffice.domain.execution.TaskExecution;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
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

/**
 * TaskExecution persistence adapter가 후속 실행 흐름에서 사용할 실행 기록을 저장하는지 검증한다.
 */
@DataJpaTest
@Import({TaskPersistenceAdapter.class, TaskExecutionPersistenceAdapter.class})
class TaskExecutionPersistenceAdapterTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private TaskJpaRepository taskJpaRepository;

    @Autowired
    private TaskExecutionJpaRepository taskExecutionJpaRepository;

    @Autowired
    private SaveTaskPort saveTaskPort;

    @Autowired
    private SaveTaskExecutionPort saveTaskExecutionPort;

    @Test
    void savePersistsQueuedTaskExecution() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        var task = saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "creator-google",
                "테스트 작성",
                null,
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L));

        var saved = saveTaskExecutionPort.save(TaskExecution.create(workspaceEntity.getId(), task.taskId(), 501L));

        assertThat(saved.executionId()).isNotNull();
        assertThat(saved.workspaceId()).isEqualTo(workspaceEntity.getId());
        assertThat(saved.taskId()).isEqualTo(task.taskId());
        assertThat(saved.agentId()).isEqualTo(501L);
        assertThat(saved.status()).isEqualTo(TaskExecutionStatus.QUEUED);
        assertThat(taskExecutionJpaRepository.count()).isEqualTo(1);
        assertThat(taskJpaRepository.findById(task.taskId())).isPresent();
    }

    private WorkspaceJpaEntity saveWorkspaceEntity(String googleSub) {
        MemberJpaEntity creatorEntity = memberJpaRepository.save(MemberJpaEntity.create(
                googleSub, googleSub + "@example.com", "Creator", MemberRole.USER));
        Workspace workspace =
                Workspace.create("Team", "Description", Member.createUser(googleSub, googleSub + "@example.com", "Creator"));
        return workspaceJpaRepository.save(WorkspaceJpaEntity.from(workspace, creatorEntity));
    }
}
